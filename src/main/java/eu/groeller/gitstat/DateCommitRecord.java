package eu.groeller.gitstat;

import java.time.Instant;

public record DateCommitRecord(Instant date, int commitCount, int additions, int deletions) {
    public DateCommitRecord add(DateCommitRecord adder) {
        return new DateCommitRecord(
                this.date,
                this.commitCount + adder.commitCount(),
                this.additions + adder.additions(),
                this.deletions + adder.deletions()
        );
    }
}
