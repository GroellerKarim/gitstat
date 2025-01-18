# Gitstat

A fast command-line tool to analyze Git repository statistics, showing commit counts, additions, and deletions per author.

## Overview

Gitstat analyzes the current Git repository and provides a summary of:
- Total number of commits
- Total lines added and deleted
- Per-author statistics with percentages of contribution
- All statistics for the current branch


Example output:

```
Author                          |  Commits |    Additions | Deletions
TOTAL                           |      448 |        96797 |    50573
-----------------------------------------------------------------
User1                           | 325 (73%) | 36977 (38%) | 20860 (41%)
User2                           |  95 (21%) | 38791 (40%) | 21082 (42%)
User3                           |  19 ( 4%) |  8025 ( 8%) |  4447 ( 9%)
```

## Build Requirements

### Standard JAR Build
```bash
mvn clean package
```

### Native Executable Build (Recommended)
Requires GraalVM 23.1.1 or later installed.
```bash
mvn -Pnative clean package
```

## Installation

After building, copy the native executable to your PATH:
```bash
sudo cp target/gitstat /usr/local/bin/
```

Or for user-local installation:
```bash
mkdir -p ~/bin
cp target/gitstat ~/bin/
# Add to ~/.bashrc or ~/.zshrc if not already present:
export PATH="$HOME/bin:$PATH"
```

## Usage

Navigate to any Git repository and run:
```bash
gitstat
```

The tool will analyze the current branch and display statistics for all contributors.

## Dependencies
- Java 23 or later
- Maven
- GraalVM 23.1.1 or later (for native builds)

## Features
- Fast parallel processing of Git history
- Accurate line counting including merge commits
- Memory-efficient processing of large repositories
- Native executable support for optimal performance

## Performance
Native builds offer significantly better performance compared to JVM builds, especially for large repositories. The tool uses parallel processing to analyze commits, making it efficient even for repositories with extensive history.

