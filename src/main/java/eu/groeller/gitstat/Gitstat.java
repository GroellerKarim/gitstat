package eu.groeller.gitstat;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.*;

public class Gitstat {
    private record CommitInfo(String author, int additions, int deletions, Instant dateTime) {
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        try {
            File gitDir = new File(".git");
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .build();

            try (Git git = new Git(repository);
                 ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                int concurrencyLevel = Runtime.getRuntime().availableProcessors() - 1;
                Map<String, CommitInfo> commits = new ConcurrentHashMap<>(4096, 0.75f, concurrencyLevel);
                CountDownLatch latch = new CountDownLatch(1);
                
                // Collect all commits first
                var commitList = StreamSupport.stream(git.log().call().spliterator(), false)
                    .filter(commit -> commit.getParentCount() <= 1)
                    .toList();
                
                latch = new CountDownLatch(commitList.size());
                
                // Process each commit with a virtual thread
                for (var commit : commitList) {
                    CountDownLatch finalLatch = latch;
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

                                commits.put(commit.getName(), new CommitInfo(
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
                            finalLatch.countDown();
                        }
                    });
                }

                // Wait for all commits to be processed
                latch.await();

                var authorStats = commits.values().stream()
                    .collect(groupingBy(
                        ci -> ci.author,
                        collectingAndThen(toList(), list -> new Object() {
                            final int commitCount = list.size();
                            final int additions = list.stream().mapToInt(ci -> ci.additions).sum();
                            final int deletions = list.stream().mapToInt(ci -> ci.deletions).sum();
                        })
                    ));

                int totalCommits = authorStats.values().stream()
                    .mapToInt(stats -> stats.commitCount)
                    .sum();
                int totalAdditions = authorStats.values().stream()
                    .mapToInt(stats -> stats.additions)
                    .sum();
                int totalDeletions = authorStats.values().stream()
                    .mapToInt(stats -> stats.deletions)
                    .sum();

                // Print header
                System.out.printf("%-30s | %8s | %12s | %9s%n",
                    "Author", "Commits", "Additions", "Deletions");
                System.out.println("-".repeat(65));

                // Print totals
                System.out.printf("%-30s | %8d | %12d | %9d%n",
                    "TOTAL", totalCommits, totalAdditions, totalDeletions);
                System.out.println("-".repeat(65));

                // Print per-author statistics
                authorStats.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().commitCount, a.getValue().commitCount))
                    .forEach(entry -> {
                        var stats = entry.getValue();
                        System.out.printf("%-30s | %3d (%2d%%) | %6d (%2d%%) | %6d (%2d%%)%n",
                            entry.getKey(),
                            stats.commitCount,
                            Math.round((stats.commitCount * 100.0f) / totalCommits),
                            stats.additions,
                            Math.round((stats.additions * 100.0f) / totalAdditions),
                            stats.deletions,
                            Math.round((stats.deletions * 100.0f) / totalDeletions));
                    });

                long endTime = System.currentTimeMillis();
                System.out.printf("%nTime taken: %.2f seconds%n", (endTime - startTime) / 1000.0);
            }
        } catch (IOException | GitAPIException | InterruptedException e) {
            e.printStackTrace();
        }
    }
} 