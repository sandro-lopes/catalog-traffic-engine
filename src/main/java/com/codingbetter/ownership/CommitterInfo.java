package com.codingbetter.ownership;

/**
 * Informações de um committer (contribuidor).
 */
public class CommitterInfo {
    private final String id; // GitHub username/login
    private final String email;
    private final String name;
    private final int commitCount;
    private final double weightedScore;

    public CommitterInfo(String id, String email, String name, int commitCount, double weightedScore) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.commitCount = commitCount;
        this.weightedScore = weightedScore;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public double getWeightedScore() {
        return weightedScore;
    }
}

