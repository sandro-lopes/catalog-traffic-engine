package com.codingbetter.finops;

import com.codingbetter.adapters.azure.AzureCostClient;
import com.codingbetter.adapters.azure.AzureResourceMapper;
import com.codingbetter.adapters.dynatrace.DynatraceClient;
import com.codingbetter.schemas.v1.CostOptimizationRecommendation;
import com.codingbetter.schemas.v1.ServiceFinOpsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gera recomendações de otimização de custos.
 * Combina análise de utilização com cálculos de economia.
 */
@Component
public class OptimizationRecommender {

    private static final Logger logger = LoggerFactory.getLogger(OptimizationRecommender.class);

    private final ResourceUtilizationAnalyzer utilizationAnalyzer;
    private final CostCalculator costCalculator;
    private final DynatraceClient dynatraceClient;
    private final AzureCostClient azureCostClient;
    private final AzureResourceMapper resourceMapper;

    public OptimizationRecommender(
            ResourceUtilizationAnalyzer utilizationAnalyzer,
            CostCalculator costCalculator,
            DynatraceClient dynatraceClient,
            AzureCostClient azureCostClient,
            AzureResourceMapper resourceMapper) {
        this.utilizationAnalyzer = utilizationAnalyzer;
        this.costCalculator = costCalculator;
        this.dynatraceClient = dynatraceClient;
        this.azureCostClient = azureCostClient;
        this.resourceMapper = resourceMapper;
    }

    /**
     * Gera recomendação completa para um serviço.
     */
    public Mono<CostOptimizationRecommendation> generateRecommendation(
            String serviceId,
            Instant startTime,
            Instant endTime) {

        logger.info("Gerando recomendação para serviço: {}", serviceId);

        long startMillis = startTime.toEpochMilli();
        long endMillis = endTime.toEpochMilli();

        // 1. Coleta métricas de recursos
        Mono<DynatraceClient.ResourceMetrics> resourceMetricsMono = 
                dynatraceClient.getResourceMetrics(serviceId, startMillis, endMillis);

        // 2. Coleta detalhes da entidade
        Mono<DynatraceClient.EntityDetails> entityDetailsMono = 
                dynatraceClient.getServiceEntityDetails(serviceId);

        // 3. Analisa utilização
        Mono<ServiceFinOpsMetrics> finOpsMetricsMono = resourceMetricsMono
                .zipWith(entityDetailsMono)
                .map(tuple -> utilizationAnalyzer.analyzeUtilization(
                        serviceId, tuple.getT1(), tuple.getT2()));

        // 4. Obtém custo atual do Azure
        Mono<Optional<String>> azureResourceIdMono = Mono.fromCallable(() -> 
                resourceMapper.mapToAzureResourceId(serviceId).orElse(null))
                .map(Optional::ofNullable);

        Mono<AzureCostClient.ResourceCost> azureCostMono = azureResourceIdMono
                .flatMap(resourceIdOpt -> {
                    if (resourceIdOpt.isEmpty()) {
                        logger.warn("Azure ResourceId não encontrado para serviço: {}", serviceId);
                        return Mono.just((AzureCostClient.ResourceCost) null);
                    }
                    LocalDate startDate = startTime.atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate endDate = endTime.atZone(ZoneId.systemDefault()).toLocalDate();
                    return azureCostClient.getResourceCost(resourceIdOpt.get(), startDate, endDate);
                });

        Mono<AzureCostClient.ResourceDetails> resourceDetailsMono = azureResourceIdMono
                .flatMap(resourceIdOpt -> {
                    if (resourceIdOpt.isEmpty()) {
                        return Mono.just((AzureCostClient.ResourceDetails) null);
                    }
                    return azureCostClient.getResourceDetails(resourceIdOpt.get());
                });

        // 5. Combina tudo e gera recomendação
        return Mono.zip(finOpsMetricsMono, azureCostMono, resourceDetailsMono)
                .map(tuple -> {
                    ServiceFinOpsMetrics finOpsMetrics = tuple.getT1();
                    AzureCostClient.ResourceCost azureCost = tuple.getT2();
                    AzureCostClient.ResourceDetails resourceDetails = tuple.getT3();

                    // Calcula custos
                    BigDecimal currentCost = costCalculator.calculateCurrentCost(azureCost);
                    BigDecimal optimizedCost = costCalculator.calculateOptimizedCost(
                            finOpsMetrics.getRecommendation(),
                            resourceDetails,
                            finOpsMetrics.getCurrentResources());

                    // Calcula economia
                    CostOptimizationRecommendation recommendation = costCalculator.calculateSavings(
                            serviceId,
                            currentCost,
                            optimizedCost,
                            finOpsMetrics.getRecommendation());

                    // Adiciona informações adicionais
                    recommendation.setCurrentResources(finOpsMetrics.getCurrentResources());
                    recommendation.setRationale(generateRationale(finOpsMetrics, recommendation));
                    recommendation.setRisks(identifyRisks(finOpsMetrics, recommendation));

                    // Calcula ROI (assumindo custo de implementação zero para simplificar)
                    BigDecimal implementationCost = BigDecimal.ZERO;
                    CostOptimizationRecommendation.ROIAnalysis roi = 
                            costCalculator.calculateROI(implementationCost, recommendation.getMonthlySavings());
                    recommendation.setRoi(roi);

                    logger.info("Recomendação gerada para {}: {} - Economia: ${}/mês",
                            serviceId, recommendation.getType(), recommendation.getMonthlySavings());

                    return recommendation;
                })
                .doOnError(error -> logger.error("Erro ao gerar recomendação para serviço: {}", serviceId, error));
    }

    private String generateRationale(
            ServiceFinOpsMetrics finOpsMetrics,
            CostOptimizationRecommendation recommendation) {

        StringBuilder rationale = new StringBuilder();

        if (recommendation.getType() == null) {
            return "Sem recomendação de otimização. Utilização dentro dos limites esperados.";
        }

        ServiceFinOpsMetrics.UtilizationPattern pattern = finOpsMetrics.getUtilizationPattern();
        ServiceFinOpsMetrics.UtilizationMetrics cpu = finOpsMetrics.getCpuUtilization();
        ServiceFinOpsMetrics.UtilizationMetrics memory = finOpsMetrics.getMemoryUtilization();

        rationale.append(String.format(
                "Utilização média: %.2f%%. CPU: %.2f%% (baixa utilização: %.2f%% do tempo). " +
                "Memória: %.2f%% (baixa utilização: %.2f%% do tempo). ",
                pattern.getAvgUtilizationPercent(),
                cpu.getAverage(),
                cpu.getLowUtilizationPercent(),
                memory.getAverage(),
                memory.getLowUtilizationPercent()));

        switch (recommendation.getType()) {
            case DOWNSCALE:
                rationale.append("Recomendação: Reduzir recursos (downscale) devido à subutilização consistente.");
                break;
            case RIGHTSIZE:
                rationale.append("Recomendação: Reduzir tamanho do SKU (rightsize) devido à subutilização extrema.");
                break;
            case DECOMMISSION:
                rationale.append("Recomendação: Descomissionar serviço devido à ausência de tráfego.");
                break;
        }

        rationale.append(String.format(" Economia potencial: $%.2f/mês (%.2f%%).",
                recommendation.getMonthlySavings().doubleValue(),
                recommendation.getSavingsPercent()));

        return rationale.toString();
    }

    private List<String> identifyRisks(
            ServiceFinOpsMetrics finOpsMetrics,
            CostOptimizationRecommendation recommendation) {

        List<String> risks = new ArrayList<>();

        if (recommendation.getType() == null) {
            return risks;
        }

        // Riscos genéricos
        if (recommendation.getConfidence() == 
                ServiceFinOpsMetrics.OptimizationRecommendation.ConfidenceLevel.LOW) {
            risks.add("Confiança baixa na recomendação. Revisar métricas antes de aplicar.");
        }

        // Riscos específicos por tipo
        switch (recommendation.getType()) {
            case DOWNSCALE:
                risks.add("Redução de recursos pode impactar performance em picos de tráfego.");
                risks.add("Monitorar métricas após aplicação da otimização.");
                break;
            case RIGHTSIZE:
                risks.add("Downgrade de SKU pode reduzir recursos disponíveis (CPU, memória, storage).");
                risks.add("Verificar se o novo SKU atende aos requisitos mínimos do serviço.");
                break;
            case DECOMMISSION:
                risks.add("Verificar se o serviço não é usado por outros sistemas ou processos.");
                risks.add("Garantir backup de dados antes do descomissionamento.");
                break;
        }

        return risks;
    }
}

