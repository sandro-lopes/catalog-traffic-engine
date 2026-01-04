package com.codingbetter.ownership;

/**
 * Resultado de uma estratégia de inferência de ownership.
 */
public class InferenceResult {

    private final String owner;
    private final String team;
    private final String area;
    private final double confidence;
    private final String strategy;

    public InferenceResult(String owner, String team, String area, double confidence, String strategy) {
        this.owner = owner;
        this.team = team;
        this.area = area;
        this.confidence = confidence;
        this.strategy = strategy;
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

