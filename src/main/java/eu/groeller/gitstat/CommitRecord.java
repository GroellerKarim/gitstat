package eu.groeller.gitstat;

import java.time.Instant;

public record CommitRecord(String author, int additions, int deletions, Instant dateTime)
{ }
