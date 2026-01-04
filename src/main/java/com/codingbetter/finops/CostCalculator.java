package com.codingbetter.finops;

import com.codingbetter.adapters.azure.AzureCostClient;
import com.codingbetter.schemas.v1.CostOptimizationRecommendation;
import com.codingbetter.schemas.v1.ServiceFinOpsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula custos atuais, otimizados e economia.
 * Inclui cálculos de ROI e payback period.
 */
@Component
public class CostCalculator {

    private static final Logger logger = LoggerFactory.getLogger(CostCalculator.class);

    @Value("${pricing.azure.app-service.basic.monthly:13.14}")
    private BigDecimal appServiceBasicMonthly;

    @Value("${pricing.azure.app-service.standard.monthly:54.75}")
    private BigDecimal appServiceStandardMonthly;

    @Value("${pricing.azure.app-service.premium.monthly:219.00}")
    private BigDecimal appServicePremiumMonthly;

    @Value("${pricing.azure.container-instances.cpu-hour:0.000012}")
    private BigDecimal containerCpuHour;

    @Value("${pricing.azure.container-instances.memory-gb-hour:0.0000015}")
    private BigDecimal containerMemoryGbHour;

    /**
     * Calcula custo atual de um recurso.
     */
    public BigDecimal calculateCurrentCost(AzureCostClient.ResourceCost azureCost) {
        if (azureCost == null) {
            return BigDecimal.ZERO;
        }
        return azureCost.getMonthlyCost();
    }

    /**
     * Calcula custo otimizado baseado na recomendação.
     */
    public BigDecimal calculateOptimizedCost(
            ServiceFinOpsMetrics.OptimizationRecommendation recommendation,
            AzureCostClient.ResourceDetails resourceDetails,
            ServiceFinOpsMetrics.ResourceSpec currentResources) {

        if (recommendation == null || recommendation.getType() == null) {
            return calculateCurrentCost(null); // Sem otimização
        }

        ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType type = recommendation.getType();

        if (type == ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType.DECOMMISSION) {
            return BigDecimal.ZERO;
        }

        // Para DOWNSCALE e RIGHTSIZE, calcula custo com recursos sugeridos
        if (recommendation.getSuggestedResources() != null) {
            return calculateCostForResources(
                    recommendation.getSuggestedResources(),
                    resourceDetails);
        }

        // Fallback: estima redução baseada no tipo de recomendação
        if (type == ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType.RIGHTSIZE) {
            return estimateRightsizeCost(resourceDetails);
        } else if (type == ServiceFinOpsMetrics.OptimizationRecommendation.RecommendationType.DOWNSCALE) {
            return estimateDownscaleCost(resourceDetails, currentResources);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Calcula economia (atual - otimizado).
     */
    public CostOptimizationRecommendation calculateSavings(
            String serviceId,
            BigDecimal currentCost,
            BigDecimal optimizedCost,
            ServiceFinOpsMetrics.OptimizationRecommendation recommendation) {

        CostOptimizationRecommendation costRec = new CostOptimizationRecommendation();
        costRec.setServiceId(serviceId);

        BigDecimal savings = currentCost.subtract(optimizedCost);
        BigDecimal annualSavings = savings.multiply(BigDecimal.valueOf(12));

        double savingsPercent = currentCost.compareTo(BigDecimal.ZERO) > 0
                ? savings.divide(currentCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        costRec.setCurrentMonthlyCost(currentCost);
        costRec.setOptimizedMonthlyCost(optimizedCost);
        costRec.setMonthlySavings(savings);
        costRec.setAnnualSavings(annualSavings);
        costRec.setSavingsPercent(savingsPercent);

        if (recommendation != null) {
            costRec.setType(recommendation.getType());
            costRec.setConfidence(recommendation.getConfidence());
            costRec.setSuggestedResources(recommendation.getSuggestedResources());
        }

        logger.info("Economia calculada para {}: ${}/mês ({}%), ${}/ano",
                serviceId, savings, String.format("%.2f", savingsPercent), annualSavings);

        return costRec;
    }

    /**
     * Calcula ROI e payback period.
     */
    public CostOptimizationRecommendation.ROIAnalysis calculateROI(
            BigDecimal implementationCost,
            BigDecimal monthlySavings) {

        CostOptimizationRecommendation.ROIAnalysis roi = new CostOptimizationRecommendation.ROIAnalysis();
        roi.setImplementationCost(implementationCost);

        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            roi.setRoiPercent(BigDecimal.ZERO);
            roi.setPaybackPeriodMonths(Double.POSITIVE_INFINITY);
            return roi;
        }

        BigDecimal annualSavings = monthlySavings.multiply(BigDecimal.valueOf(12));
        BigDecimal netSavings = annualSavings.subtract(implementationCost);

        if (implementationCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal roiPercent = netSavings.divide(implementationCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            roi.setRoiPercent(roiPercent);
        } else {
            // Sem custo de implementação = ROI infinito
            roi.setRoiPercent(BigDecimal.valueOf(Double.POSITIVE_INFINITY));
        }

        double paybackMonths = implementationCost.divide(monthlySavings, 2, RoundingMode.HALF_UP)
                .doubleValue();
        roi.setPaybackPeriodMonths(paybackMonths);

        return roi;
    }

    private BigDecimal calculateCostForResources(
            ServiceFinOpsMetrics.ResourceSpec resources,
            AzureCostClient.ResourceDetails resourceDetails) {

        String resourceType = resources.getResourceType();
        if (resourceType == null && resourceDetails != null) {
            resourceType = resourceDetails.getResourceType();
        }

        if (resourceType != null && resourceType.contains("Microsoft.Web/sites")) {
            // App Service - usa preços fixos por SKU
            String sku = resources.getResourceType(); // Simplificado
            return getAppServicePrice(sku);
        } else if (resourceType != null && resourceType.contains("Container")) {
            // Container Instances - calcula por CPU e memória
            double cpuHours = (resources.getCpuCores() != null ? resources.getCpuCores() : 1.0) * 730; // 730 horas/mês
            double memoryGbHours = (resources.getMemoryGB() != null ? resources.getMemoryGB() : 1.0) * 730;
            
            BigDecimal cpuCost = containerCpuHour.multiply(BigDecimal.valueOf(cpuHours));
            BigDecimal memoryCost = containerMemoryGbHour.multiply(BigDecimal.valueOf(memoryGbHours));
            
            return cpuCost.add(memoryCost).multiply(
                    BigDecimal.valueOf(resources.getInstanceCount() != null ? resources.getInstanceCount() : 1));
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal estimateRightsizeCost(AzureCostClient.ResourceDetails resourceDetails) {
        if (resourceDetails == null) {
            return BigDecimal.ZERO;
        }

        String currentSku = resourceDetails.getSkuTier() != null 
                ? resourceDetails.getSkuTier() 
                : resourceDetails.getSkuName();

        // Downgrade de SKU: Premium -> Standard -> Basic
        if (currentSku != null && currentSku.toLowerCase().contains("premium")) {
            return appServiceStandardMonthly;
        } else if (currentSku != null && currentSku.toLowerCase().contains("standard")) {
            return appServiceBasicMonthly;
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal estimateDownscaleCost(
            AzureCostClient.ResourceDetails resourceDetails,
            ServiceFinOpsMetrics.ResourceSpec currentResources) {

        // Reduz instâncias pela metade (simplificado)
        BigDecimal currentCost = getAppServicePrice(
                resourceDetails != null ? resourceDetails.getSkuTier() : null);
        
        int currentInstances = currentResources != null && currentResources.getInstanceCount() != null
                ? currentResources.getInstanceCount()
                : 1;
        
        int optimizedInstances = Math.max(1, currentInstances / 2);
        
        return currentCost.multiply(BigDecimal.valueOf(optimizedInstances))
                .divide(BigDecimal.valueOf(currentInstances), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getAppServicePrice(String sku) {
        if (sku == null) {
            return appServiceStandardMonthly; // Default
        }

        String skuLower = sku.toLowerCase();
        if (skuLower.contains("premium")) {
            return appServicePremiumMonthly;
        } else if (skuLower.contains("standard")) {
            return appServiceStandardMonthly;
        } else if (skuLower.contains("basic")) {
            return appServiceBasicMonthly;
        }

        return appServiceStandardMonthly; // Default
    }
}

