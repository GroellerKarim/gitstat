package eu.groeller.gitstat;

import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Map;

public class GitContributionPresenter {
    private final Map<String, AuthorRecord> authorStats;

    public GitContributionPresenter(Map<String, AuthorRecord> authorStats) {
        this.authorStats = authorStats;
    }

    public Table createTable() {
        int totalCommits = calculateTotalCommits();
        int totalAdditions = calculateTotalAdditions();
        int totalDeletions = calculateTotalDeletions();

        Table authorTable = initializeTable();
        populateTable(authorTable, totalCommits, totalAdditions, totalDeletions);
        
        authorTable = authorTable.sortDescendingOn("Hidden Commits");
        return authorTable.removeColumns("Hidden Commits");
    }

    private int calculateTotalCommits() {
        return authorStats.values().stream()
                .mapToInt(AuthorRecord::commitCount)
                .sum();
    }

    private int calculateTotalAdditions() {
        return authorStats.values().stream()
                .mapToInt(AuthorRecord::additionsSum)
                .sum();
    }

    private int calculateTotalDeletions() {
        return authorStats.values().stream()
                .mapToInt(AuthorRecord::deletionsSum)
                .sum();
    }

    private Table initializeTable() {
        return Table.create("Git Statistics")
                .addColumns(
                        StringColumn.create("Author"),
                        IntColumn.create("Hidden Commits"),
                        StringColumn.create("Commits"),
                        StringColumn.create("Additions"),
                        StringColumn.create("Deletions")
                );
    }

    private void populateTable(Table table, int totalCommits, int totalAdditions, int totalDeletions) {
        authorStats.forEach((author, stats) -> {
            table.stringColumn("Author").append(author);
            table.intColumn("Hidden Commits").append(stats.commitCount());
            table.stringColumn("Commits").append(formatPercentage(stats.commitCount(), totalCommits));
            table.stringColumn("Additions").append(formatPercentage(stats.additionsSum(), totalAdditions));
            table.stringColumn("Deletions").append(formatPercentage(stats.deletionsSum(), totalDeletions));
        });
    }

    private String formatPercentage(int value, int total) {
        double percentage = Math.round((value * 100.0) / total * 10.0) / 10.0;
        return String.format("%d (%.1f)", value, percentage);
    }
} 