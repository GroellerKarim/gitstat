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

public class Gitstat {
    public static void main(String[] args) {
        try {
            File gitDir = new File(".git");
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .build();

            try (Git git = new Git(repository)) {
                Map<String, AuthorStats> statsMap = new HashMap<>();

                git.log().call().forEach(commit -> {
                    String author = commit.getAuthorIdent().getName();
                    statsMap.computeIfAbsent(author, k -> new AuthorStats());
                    AuthorStats stats = statsMap.get(author);
                    stats.commitCount++;

                    // Calculate diff stats
                    try {
                        if (commit.getParentCount() > 0) {
                            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                            df.setRepository(repository);
                            for (DiffEntry diff : df.scan(commit.getParent(0).getTree(), commit.getTree())) {
                                for(Edit edit : df.toFileHeader(diff).toEditList()) {
                                    if (edit.getType() == Edit.Type.INSERT) {
                                        stats.additions += edit.getLengthB();
                                    }
                                    else if (edit.getType() == Edit.Type.DELETE) {
                                        stats.deletions += edit.getLengthA();
                                    }
                                    else if (edit.getType() == Edit.Type.REPLACE) {
                                        stats.additions += edit.getLengthB();
                                        stats.deletions += edit.getLengthA();
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // Print header
                System.out.printf("%-30s | %8s | %10s | %10s%n", 
                    "Author", "Commits", "Additions", "Deletions");
                System.out.println("-".repeat(65));  // Separator line

                // Print data
                statsMap.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().commitCount, a.getValue().commitCount))
                    .forEach(entry -> 
                        System.out.printf("%-30s | %8d | %10d | %10d%n",
                            entry.getKey(), 
                            entry.getValue().commitCount, 
                            entry.getValue().additions, 
                            entry.getValue().deletions));
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    private static class AuthorStats {
        int commitCount = 0;
        int additions = 0;
        int deletions = 0;
    }
} 