package eu.groeller.gitstat;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Gitstat {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        try {
            File gitDir = new File(".git");
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .build();

            try (GitRepositoryAnalyzer analyzer = new GitRepositoryAnalyzer(repository)) {
                Map<String, AuthorRecord> authorStats = analyzer.analyzeRepository();
                
                GitContributionPresenter presenter = new GitContributionPresenter(authorStats);
                Table resultTable = presenter.createTable();
                
                System.out.println(resultTable.print());

                long endTime = System.currentTimeMillis();
                System.out.printf("%nTime taken: %.2f seconds%n", (endTime - startTime) / 1000.0);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
} 