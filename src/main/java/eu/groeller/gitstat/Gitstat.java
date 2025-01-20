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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

public class Gitstat {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        try {
            File gitDir = new File(".git");
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .build();

            try (Git git = new Git(repository)) {
                Map<String, AuthorStats> statsMap = new ConcurrentHashMap<>();

                // Convert Iterable to Stream and parallelize
                StreamSupport.stream(git.log().call().spliterator(), true).forEach(commit -> {
                    // Skip merge commits (commits with more than one parent)
                    if (commit.getParentCount() > 1) {
                        return;
                    }

                    // Create a new DiffFormatter for each thread
                    DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                    df.setRepository(repository);

                    String author = commit.getAuthorIdent().getName();
                    AuthorStats stats = statsMap.computeIfAbsent(author, k -> new AuthorStats());
                    stats.commitCount.incrementAndGet();

                    // Calculate diff stats
                    try {
                        if (commit.getParentCount() > 0) {
                            for (DiffEntry diff : df.scan(commit.getParent(0).getTree(), commit.getTree())) {
                                for(Edit edit : df.toFileHeader(diff).toEditList()) {
                                    stats.updateStats(edit);
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.printf("Warning: Could not process commit %s: %s%n", 
                            commit.getName(), e.getMessage());
                    } finally {
                        df.close();
                    }
                });

                // Print header
                System.out.printf("%-30s | %10s | %12s | %9s%n",
                    "Author", "Commits", "Additions", "Deletions");
                System.out.println("-".repeat(65));

                // Calculate and print totals
                int totalCommits = statsMap.values().stream()
                    .mapToInt(stats -> stats.commitCount.get())
                    .sum();
                int totalAdditions = statsMap.values().stream()
                    .mapToInt(stats -> stats.additions.get())
                    .sum();
                int totalDeletions = statsMap.values().stream()
                    .mapToInt(stats -> stats.deletions.get())
                    .sum();

                System.out.printf("%-30s | %10d | %14d | %11d%n",
                    "TOTAL", totalCommits, totalAdditions, totalDeletions);
                System.out.println("-".repeat(65));  // Separator line

                // Print individual data with percentages
                statsMap.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().commitCount.get(), a.getValue().commitCount.get()))
                    .forEach(entry -> 
                        System.out.printf("%-30s | %5d (%2d%%) | %8d (%2d%%) | %8d (%2d%%)%n",
                            entry.getKey(), 
                            entry.getValue().commitCount.get(),
                            Math.round((entry.getValue().commitCount.get() * 100.0f) / totalCommits),
                            entry.getValue().additions.get(),
                            Math.round((entry.getValue().additions.get() * 100.0f) / totalAdditions),
                            entry.getValue().deletions.get(),
                            Math.round((entry.getValue().deletions.get() * 100.0f) / totalDeletions)));

                long endTime = System.currentTimeMillis();
                System.out.printf("%nTime taken: %.2f seconds%n", (endTime - startTime) / 1000.0);
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    private static class AuthorStats {
        private final AtomicInteger commitCount = new AtomicInteger(0);
        private final AtomicInteger additions = new AtomicInteger(0);
        private final AtomicInteger deletions = new AtomicInteger(0);

        void updateStats(Edit edit) {
            switch (edit.getType()) {
                case INSERT -> additions.getAndAdd(edit.getLengthB());
                case DELETE -> deletions.getAndAdd(edit.getLengthA());
                case REPLACE -> {
                    additions.getAndAdd(edit.getLengthB());
                    deletions.getAndAdd(edit.getLengthA());
                }
            }
        }

        int getCommitCount() { return commitCount.get(); }
        int getAdditions() { return additions.get(); }
        int getDeletions() { return deletions.get(); }
    }
} 