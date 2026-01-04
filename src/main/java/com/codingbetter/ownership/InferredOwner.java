package com.codingbetter.ownership;

/**
 * Owner inferido com metadados de confiança.
 */
public class InferredOwner {

    private final String owner;
    private final String team;
    private final String area;
    private final double confidence;
    private final String strategy;

    public InferredOwner(String owner, String team, String area, double confidence, String strategy) {
        this.owner = owner;
        this.team = team;
        this.area = area;
        this.confidence = confidence;
        this.strategy = strategy;
    }

    public static InferredOwner unknown() {
        return new InferredOwner(null, null, null, 0.0, "unknown");
    }

    public boolean isUnknown() {
        return owner == null || confidence == 0.0;
    }

    public String getOwner() {
        return owner;
    }

    public String getTeam() {
        return team;
    }

    public String getArea() {
        return area;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getStrategy() {
        return strategy;
    }
}

