package eu.groeller.gitstat;

import java.util.List;

public record AuthorRecord(String author, int commitCount, int additionsSum, int deletionsSum){
}
