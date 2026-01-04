package com.codingbetter.finops;

import com.codingbetter.schemas.v1.CostOptimizationRecommendation;
import com.codingbetter.schemas.v1.ServiceFinOpsMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gera relatórios agregados de economia e otimização.
 * Suporta exportação em JSON e CSV.
 */
@Component
public class EconomyReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(EconomyReportGenerator.class);

    private final ObjectMapper objectMapper;

    public EconomyReportGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Gera relatório agregado de todas as recomendações.
     */
    public EconomyReport generateReport(
            Instant snapshotDate,
            List<CostOptimizationRecommendation> recommendations) {

        logger.info("Gerando relatório de economia para {} recomendações", recommendations.size());

        EconomyReport report = new EconomyReport();
        report.setSnapshotDate(snapshotDate);
        report.setTotalServicesAnalyzed(recommendations.size());

        // Filtra apenas recomendações válidas (com tipo definido)
        List<CostOptimizationRecommendation> validRecommendations = recommendations.stream()
                .filter(rec -> rec.getType() != null)
                .collect(Collectors.toList());

        report.setServicesWithOpportunities(validRecommendations.size());

        // Calcula totais de economia
        BigDecimal totalMonthlySavings = validRecommendations.stream()
                .map(CostOptimizationRecommendation::getMonthlySavings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAnnualSavings = totalMonthlySavings.multiply(BigDecimal.valueOf(12));

        report.setTotalMonthlySavings(totalMonthlySavings);
        report.setTotalAnnualSavings(totalAnnualSavings);

        // Breakdown por tipo de recomendação
        Map<ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType, Long> byType = validRecommendations.stream()
                .collect(Collectors.groupingBy(
                        rec -> rec.getType(),
                        Collectors.counting()));

        report.setRecommendationsByType(byType);

        // Breakdown por classificação (se disponível)
        // report.setBreakdownByClassification(...);

        // Top 10 oportunidades
        List<CostOptimizationRecommendation> topOpportunities = validRecommendations.stream()
                .sorted((a, b) -> b.getAnnualSavings().compareTo(a.getAnnualSavings()))
                .limit(10)
                .collect(Collectors.toList());

        report.setTopOpportunities(topOpportunities);

        logger.info("Relatório gerado: {} serviços com oportunidades, ${}/ano de economia potencial",
                validRecommendations.size(), totalAnnualSavings);

        return report;
    }

    /**
     * Exporta relatório em formato JSON.
     */
    public String exportToJson(EconomyReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (Exception e) {
            logger.error("Erro ao exportar relatório para JSON", e);
            throw new RuntimeException("Erro ao exportar relatório", e);
        }
    }

    /**
     * Exporta relatório em formato CSV.
     */
    public String exportToCsv(List<CostOptimizationRecommendation> recommendations) {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Service ID,Type,Current Monthly Cost,Optimized Monthly Cost,")
           .append("Monthly Savings,Annual Savings,Savings %,Confidence\n");

        // Data
        for (CostOptimizationRecommendation rec : recommendations) {
            if (rec.getType() == null) {
                continue;
            }

            csv.append(escapeCsv(rec.getServiceId())).append(",")
               .append(rec.getType()).append(",")
               .append(rec.getCurrentMonthlyCost()).append(",")
               .append(rec.getOptimizedMonthlyCost()).append(",")
               .append(rec.getMonthlySavings()).append(",")
               .append(rec.getAnnualSavings()).append(",")
               .append(String.format("%.2f", rec.getSavingsPercent())).append(",")
               .append(rec.getConfidence()).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static class EconomyReport {
        private Instant snapshotDate;
        private int totalServicesAnalyzed;
        private int servicesWithOpportunities;
        private BigDecimal totalMonthlySavings;
        private BigDecimal totalAnnualSavings;
        private Map<ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType, Long> recommendationsByType;
        private List<CostOptimizationRecommendation> topOpportunities;

        // Getters and Setters
        public Instant getSnapshotDate() {
            return snapshotDate;
        }

        public void setSnapshotDate(Instant snapshotDate) {
            this.snapshotDate = snapshotDate;
        }

        public int getTotalServicesAnalyzed() {
            return totalServicesAnalyzed;
        }

        public void setTotalServicesAnalyzed(int totalServicesAnalyzed) {
            this.totalServicesAnalyzed = totalServicesAnalyzed;
        }

        public int getServicesWithOpportunities() {
            return servicesWithOpportunities;
        }

        public void setServicesWithOpportunities(int servicesWithOpportunities) {
            this.servicesWithOpportunities = servicesWithOpportunities;
        }

        public BigDecimal getTotalMonthlySavings() {
            return totalMonthlySavings;
        }

        public void setTotalMonthlySavings(BigDecimal totalMonthlySavings) {
            this.totalMonthlySavings = totalMonthlySavings;
        }

        public BigDecimal getTotalAnnualSavings() {
            return totalAnnualSavings;
        }

        public void setTotalAnnualSavings(BigDecimal totalAnnualSavings) {
            this.totalAnnualSavings = totalAnnualSavings;
        }

        public Map<ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType, Long> getRecommendationsByType() {
            return recommendationsByType;
        }

        public void setRecommendationsByType(Map<ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType, Long> recommendationsByType) {
            this.recommendationsByType = recommendationsByType;
        }

        public List<CostOptimizationRecommendation> getTopOpportunities() {
            return topOpportunities;
        }

        public void setTopOpportunities(List<CostOptimizationRecommendation> topOpportunities) {
            this.topOpportunities = topOpportunities;
        }
    }
}

