package eu.groeller.gitstat;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.*;

public class GitRepositoryAnalyzer implements AutoCloseable{
    private final Repository repository;
    private final Git git;

    public GitRepositoryAnalyzer(Repository repository) {
        this.repository = repository;
        this.git = new Git(repository);
    }

    public Map<String, AuthorRecord> analyzeRepository() throws IOException, InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            int concurrencyLevel = Runtime.getRuntime().availableProcessors() - 1;
            Map<String, CommitRecord> commits = new ConcurrentHashMap<>(4096, 0.75f, concurrencyLevel);

            var commitList = StreamSupport.stream(git.log().call().spliterator(), false)
                    .filter(commit -> commit.getParentCount() <= 1)
                    .toList();

            CountDownLatch latch = new CountDownLatch(commitList.size());

            for (var commit : commitList) {
                executor.submit(() -> {
                    try {
                        if (commit.getParentCount() > 0) {
                            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                            df.setRepository(repository);

                            int additions = 0;
                            int deletions = 0;

                            for (DiffEntry diff : df.scan(commit.getParent(0).getTree(), commit.getTree())) {
                                for (Edit edit : df.toFileHeader(diff).toEditList()) {
                                    switch (edit.getType()) {
                                        case INSERT -> additions += edit.getLengthB();
                                        case DELETE -> deletions += edit.getLengthA();
                                        case REPLACE -> {
                                            additions += edit.getLengthB();
                                            deletions += edit.getLengthA();
                                        }
                                    }
                                }
                            }

                            commits.put(commit.getName(), new CommitRecord(
                                    commit.getAuthorIdent().getName(),
                                    additions,
                                    deletions,
                                    Instant.ofEpochSecond(commit.getCommitTime())
                            ));

                            df.close();
                        }
                    } catch (IOException e) {
                        System.err.printf("Warning: Could not process commit %s: %s%n",
                                commit.getName(), e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();

            return commits.values().stream()
                    .collect(groupingBy(
                            CommitRecord::author,
                            collectingAndThen(toList(), list -> new AuthorRecord(
                                    list.getFirst().author(),
                                    list.size(),
                                    list.stream().mapToInt(CommitRecord::additions).sum(),
                                    list.stream().mapToInt(CommitRecord::deletions).sum()))
                    ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze repository", e);
        }
    }

    @Override
    public void close() {
        git.close();
    }
} 