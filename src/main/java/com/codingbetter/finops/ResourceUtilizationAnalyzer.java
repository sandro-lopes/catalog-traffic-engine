package com.codingbetter.finops;

import com.codingbetter.adapters.dynatrace.DynatraceClient;
import com.codingbetter.schemas.v1.ServiceFinOpsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Analisa padrões de utilização de recursos.
 * Identifica oportunidades de otimização baseadas em subutilização.
 */
@Component
public class ResourceUtilizationAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ResourceUtilizationAnalyzer.class);

    @Value("${finops.low-utilization-threshold:20}")
    private double lowUtilizationThreshold;

    @Value("${finops.downscale-threshold:30}")
    private double downscaleThreshold;

    @Value("${finops.rightsize-threshold:10}")
    private double rightsizeThreshold;

    /**
     * Analisa utilização e gera métricas FinOps.
     */
    public ServiceFinOpsMetrics analyzeUtilization(
            String serviceId,
            DynatraceClient.ResourceMetrics resourceMetrics,
            DynatraceClient.EntityDetails entityDetails) {

        logger.debug("Analisando utilização para serviço: {}", serviceId);

        ServiceFinOpsMetrics metrics = new ServiceFinOpsMetrics();
        metrics.setServiceId(serviceId);

        // Analisa CPU
        ServiceFinOpsMetrics.UtilizationMetrics cpuMetrics = analyzeUtilizationValues(
                resourceMetrics.getCpuValues(), "CPU");
        metrics.setCpuUtilization(cpuMetrics);

        // Analisa Memória
        ServiceFinOpsMetrics.UtilizationMetrics memoryMetrics = analyzeUtilizationValues(
                resourceMetrics.getMemoryValues(), "Memory");
        metrics.setMemoryUtilization(memoryMetrics);

        // Identifica padrão de utilização
        ServiceFinOpsMetrics.UtilizationPattern pattern = identifyUtilizationPattern(
                cpuMetrics, memoryMetrics);
        metrics.setUtilizationPattern(pattern);

        // Gera recomendação
        ServiceFinOpsMetrics.OptimizationRecommendation recommendation = identifyOptimizationOpportunity(
                pattern, cpuMetrics, memoryMetrics);
        metrics.setRecommendation(recommendation);

        return metrics;
    }

    private ServiceFinOpsMetrics.UtilizationMetrics analyzeUtilizationValues(
            List<Double> values, String metricName) {

        ServiceFinOpsMetrics.UtilizationMetrics metrics = new ServiceFinOpsMetrics.UtilizationMetrics();

        if (values == null || values.isEmpty()) {
            logger.warn("Sem valores de {} para análise", metricName);
            return metrics;
        }

        // Calcula estatísticas
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);

        double sum = sortedValues.stream().mapToDouble(Double::doubleValue).sum();
        metrics.setAverage(sum / sortedValues.size());
        metrics.setP50(percentile(sortedValues, 50));
        metrics.setP95(percentile(sortedValues, 95));
        metrics.setP99(percentile(sortedValues, 99));
        metrics.setPeakUtilization(Collections.max(sortedValues));

        // Calcula percentual de tempo com baixa utilização (< 20%)
        long lowUtilizationCount = sortedValues.stream()
                .filter(v -> v < lowUtilizationThreshold)
                .count();
        metrics.setLowUtilizationPercent((double) lowUtilizationCount / sortedValues.size() * 100);

        logger.debug("{} - Média: {:.2f}%, P95: {:.2f}%, Baixa utilização: {:.2f}%",
                metricName, metrics.getAverage(), metrics.getP95(), metrics.getLowUtilizationPercent());

        return metrics;
    }

    private double percentile(List<Double> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private ServiceFinOpsMetrics.UtilizationPattern identifyUtilizationPattern(
            ServiceFinOpsMetrics.UtilizationMetrics cpuMetrics,
            ServiceFinOpsMetrics.UtilizationMetrics memoryMetrics) {

        ServiceFinOpsMetrics.UtilizationPattern pattern = new ServiceFinOpsMetrics.UtilizationPattern();

        // Média ponderada (CPU 60%, Memory 40%)
        double avgUtilization = (cpuMetrics.getAverage() * 0.6) + (memoryMetrics.getAverage() * 0.4);
        pattern.setAvgUtilizationPercent(avgUtilization);

        // Identifica horários de pico e baixo uso (simplificado - em produção, analisar por hora)
        // Por enquanto, retorna vazio - pode ser implementado com análise temporal detalhada
        pattern.setPeakHours(new ArrayList<>());
        pattern.setLowUsageHours(new ArrayList<>());

        return pattern;
    }

    private ServiceFinOpsMetrics.OptimizationRecommendation identifyOptimizationOpportunity(
            ServiceFinOpsMetrics.UtilizationPattern pattern,
            ServiceFinOpsMetrics.UtilizationMetrics cpuMetrics,
            ServiceFinOpsMetrics.UtilizationMetrics memoryMetrics) {

        ServiceFinOpsMetrics.OptimizationRecommendation recommendation =
                new ServiceFinOpsMetrics.OptimizationRecommendation();

        double avgUtilization = pattern.getAvgUtilizationPercent();
        double cpuLowPercent = cpuMetrics.getLowUtilizationPercent();
        double memoryLowPercent = memoryMetrics.getLowUtilizationPercent();

        // Regras de recomendação
        if (avgUtilization < rightsizeThreshold && 
            (cpuLowPercent > 90 || memoryLowPercent > 90)) {
            // Muito subutilizado por muito tempo -> RIGHTSIZE (redução significativa)
            recommendation.setType(ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType.RIGHTSIZE);
            recommendation.setConfidence(ServiceFinOpsMetrics.OptimizationRecommendation.ConfidenceLevel.HIGH);
        } else if (avgUtilization < downscaleThreshold && 
                   (cpuLowPercent > 70 || memoryLowPercent > 70)) {
            // Subutilizado -> DOWNSCALE (reduzir instâncias ou recursos)
            recommendation.setType(ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType.DOWNSCALE);
            recommendation.setConfidence(ServiceFinOpsMetrics.OptimizationRecommendation.ConfidenceLevel.MEDIUM);
        } else {
            // Sem recomendação clara
            recommendation.setType(null);
            recommendation.setConfidence(ServiceFinOpsMetrics.OptimizationRecommendation.ConfidenceLevel.LOW);
        }

        return recommendation;
    }
}

