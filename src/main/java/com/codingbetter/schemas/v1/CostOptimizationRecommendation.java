package com.codingbetter.schemas.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Recomendação de otimização de custos para um serviço.
 * Inclui cálculos de economia e análise de ROI.
 */
public class CostOptimizationRecommendation {

    @NotBlank
    @JsonProperty("service.id")
    private String serviceId;

    @NotNull
    @JsonProperty("type")
    private ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType type;

    @JsonProperty("currentResources")
    private ServiceFinOpsMetrics.ResourceSpec currentResources;

    @JsonProperty("suggestedResources")
    private ServiceFinOpsMetrics.ResourceSpec suggestedResources;

    @NotNull
    @PositiveOrZero
    @JsonProperty("currentMonthlyCost")
    private BigDecimal currentMonthlyCost;

    @NotNull
    @PositiveOrZero
    @JsonProperty("optimizedMonthlyCost")
    private BigDecimal optimizedMonthlyCost;

    @NotNull
    @JsonProperty("monthlySavings")
    private BigDecimal monthlySavings;

    @NotNull
    @JsonProperty("annualSavings")
    private BigDecimal annualSavings;

    @NotNull
    @JsonProperty("savingsPercent")
    private Double savingsPercent;

    @NotNull
    @JsonProperty("confidence")
    private ServiceFinOpsMetrics.OptimizationRecommendation.ConfidenceLevel confidence;

    @JsonProperty("rationale")
    private String rationale;

    @JsonProperty("risks")
    private List<String> risks;

    @JsonProperty("roi")
    private ROIAnalysis roi;

    public CostOptimizationRecommendation() {
    }

    // Getters and Setters
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType getType() {
        return type;
    }

    public void setType(ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType type) {
        this.type = type;
    }

    public ServiceFinOpsMetrics.ResourceSpec getCurrentResources() {
        return currentResources;
    }

    public void setCurrentResources(ServiceFinOpsMetrics.ResourceSpec currentResources) {
        this.currentResources = currentResources;
    }

    public ServiceFinOpsMetrics.ResourceSpec getSuggestedResources() {
        return suggestedResources;
    }

    public void setSuggestedResources(ServiceFinOpsMetrics.ResourceSpec suggestedResources) {
        this.suggestedResources = suggestedResources;
    }

    public BigDecimal getCurrentMonthlyCost() {
        return currentMonthlyCost;
    }

    public void setCurrentMonthlyCost(BigDecimal currentMonthlyCost) {
        this.currentMonthlyCost = currentMonthlyCost;
    }

    public BigDecimal getOptimizedMonthlyCost() {
        return optimizedMonthlyCost;
    }

    public void setOptimizedMonthlyCost(BigDecimal optimizedMonthlyCost) {
        this.optimizedMonthlyCost = optimizedMonthlyCost;
    }

    public BigDecimal getMonthlySavings() {
        return monthlySavings;
    }

    public void setMonthlySavings(BigDecimal monthlySavings) {
        this.monthlySavings = monthlySavings;
    }

    public BigDecimal getAnnualSavings() {
        return annualSavings;
    }

    public void setAnnualSavings(BigDecimal annualSavings) {
        this.annualSavings = annualSavings;
    }

    public Double getSavingsPercent() {
        return savingsPercent;
    }

    public void setSavingsPercent(Double savingsPercent) {
        this.savingsPercent = savingsPercent;
    }

    public ServiceFinOpsMetrics.OptimizationRecommendation.ConfidenceLevel getConfidence() {
        return confidence;
    }

    public void setConfidence(ServiceFinOpsMetrics.OptimizationRecommendation.ConfidenceLevel confidence) {
        this.confidence = confidence;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public ROIAnalysis getRoi() {
        return roi;
    }

    public void setRoi(ROIAnalysis roi) {
        this.roi = roi;
    }

    public static class ROIAnalysis {
        private BigDecimal implementationCost;
        private BigDecimal roiPercent;
        private Double paybackPeriodMonths;

        public ROIAnalysis() {
        }

        public BigDecimal getImplementationCost() {
            return implementationCost;
        }

        public void setImplementationCost(BigDecimal implementationCost) {
            this.implementationCost = implementationCost;
        }

        public BigDecimal getRoiPercent() {
            return roiPercent;
        }

        public void setRoiPercent(BigDecimal roiPercent) {
            this.roiPercent = roiPercent;
        }

        public Double getPaybackPeriodMonths() {
            return paybackPeriodMonths;
        }

        public void setPaybackPeriodMonths(Double paybackPeriodMonths) {
            this.paybackPeriodMonths = paybackPeriodMonths;
        }
    }
}

