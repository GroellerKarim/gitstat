package eu.groeller.gitstat;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

public class GitRepositoryAnalyzer implements AutoCloseable{
    private final Repository repository;
    private final Git git;
    private final Map<ObjectId, CommitRecord> commits;

    public GitRepositoryAnalyzer(Repository repository) {
        this.repository = repository;
        this.git = new Git(repository);

        commits = new ConcurrentHashMap<>(4096, Runtime.getRuntime().availableProcessors() - 1);
    }

    public Map<String, AuthorRecord> analyzeRepository() throws IOException, InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            var commitList = StreamSupport.stream(git.log().call().spliterator(), false)
                    .filter(commit -> commit.getParentCount() <= 1)
                    .toList();
                
            CountDownLatch latch = new CountDownLatch(commitList.size());

            for (var commit : commitList) {
                executor.submit(() -> {
                    try {
                        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                        df.setRepository(repository);

                        int additions = 0;
                        int deletions = 0;

                        if (commit.getParentCount() > 0) {
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
                        } else {
                            ObjectId emptyTreeId = repository.newObjectInserter().insert(org.eclipse.jgit.lib.Constants.OBJ_TREE, new byte[0]);
                            for (DiffEntry diff : df.scan(emptyTreeId, commit.getTree())) {
                                for (Edit edit : df.toFileHeader(diff).toEditList()) {
                                    if (edit.getType() == Edit.Type.INSERT) {
                                        additions += edit.getLengthB();
                                    }
                                }
                            }
                        }

                        commits.put(commit.getId(), new CommitRecord(
                                commit.getAuthorIdent().getName(),
                                additions,
                                deletions,
                                Instant.ofEpochSecond(commit.getCommitTime())
                        ));

                        df.close();
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

    public List<DateCommitRecord> getTimeSeriesData(boolean fillGaps) {
        final LocalDateTime minDateTime = commits.values().stream()
                .map(commit -> LocalDateTime.ofInstant(commit.dateTime(), ZoneId.systemDefault())
                        .truncatedTo(ChronoUnit.DAYS)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
                .min(LocalDateTime::compareTo)
                .orElseThrow();

        final LocalDateTime maxDateTime = commits.values().stream()
                .map(commit -> LocalDateTime.ofInstant(commit.dateTime(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS))
                .max(LocalDateTime::compareTo)
                .orElseThrow();


        final Map<LocalDateTime, DateCommitRecord> timeSeriesData = commits.values().stream()
                .collect(groupingBy(
                        commit -> LocalDateTime.ofInstant(commit.dateTime(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS),
                        collectingAndThen(toList(), list -> new DateCommitRecord(
                                list.getFirst().dateTime().truncatedTo(ChronoUnit.DAYS),
                                list.size(),
                                list.stream().mapToInt(CommitRecord::additions).sum(),
                                list.stream().mapToInt(CommitRecord::deletions).sum()
                        ))
                ));

        if (!fillGaps) {
            return timeSeriesData.values().stream()
                    .sorted(comparing(DateCommitRecord::date))
                    .toList();
        }

        final var today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return Stream.iterate(minDateTime,
                        date -> date.isBefore(today) || date.equals(today),
                        date -> date.plusWeeks(1))
                .map(startDate -> {
                    final var endWeek = startDate.plusWeeks(1);
                    return Stream.iterate(startDate,
                                    date -> date.isBefore(endWeek),
                                    date -> date.plusDays(1))
                            .map(timeSeriesData::get)
                            .filter(Objects::nonNull)
                            .reduce(new DateCommitRecord(startDate.atZone(ZoneId.systemDefault()).toInstant(), 0, 0, 0), DateCommitRecord::add);
                })
                .toList();
    }

    @Override
    public void close() {
        git.close();
    }
} 