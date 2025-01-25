package eu.groeller.gitstat;

public record AuthorRecord(String author, int commitCount, int additionsSum, int deletionsSum) { }
