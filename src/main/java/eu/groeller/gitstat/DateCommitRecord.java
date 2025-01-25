package eu.groeller.gitstat;

import java.time.Instant;

public record DateCommitRecord(Instant date, int commitCount, int additions, int deletions) { }
