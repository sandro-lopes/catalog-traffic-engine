package com.codingbetter.consolidation;

import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.common.TopicPartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Coordenador do job de consolidação diário.
 * Identifica partições do tópico raw e distribui trabalho entre workers.
 */
@Component
public class ConsolidationCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(ConsolidationCoordinator.class);

    private final ConsolidationWorker worker;
    private final AdminClient adminClient;

    @Value("${consolidation.job.window-days:30}")
    private int windowDays;

    @Value("${consolidation.workers.min:10}")
    private int minWorkers;

    @Value("${consolidation.workers.max:100}")
    private int maxWorkers;

    public ConsolidationCoordinator(
            ConsolidationWorker worker,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.worker = worker;
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.adminClient = AdminClient.create(config);
    }

    /**
     * Executa job de consolidação diário.
     * Agendado para executar às 2 AM (configurável via cron).
     */
    @Scheduled(cron = "${consolidation.job.cron:0 2 * * *}")
    public void executeConsolidation() {
        logger.info("Iniciando job de consolidação diário");

        try {
            // Descobre partições do tópico
            List<Integer> partitions = discoverPartitions("governance.activity.raw");
            logger.info("Encontradas {} partições para processar", partitions.size());

            // Distribui trabalho entre workers
            List<ServiceActivitySnapshot> allSnapshots = processPartitions(partitions);

            logger.info("Job de consolidação concluído: {} snapshots gerados", allSnapshots.size());

        } catch (Exception e) {
            logger.error("Erro crítico no job de consolidação", e);
        }
    }

    /**
     * Descobre partições de um tópico.
     */
    private List<Integer> discoverPartitions(String topicName) {
        try {
            DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topicName));
            Map<String, org.apache.kafka.clients.admin.TopicDescription> topics = result.allTopicNames().get();

            org.apache.kafka.clients.admin.TopicDescription topicDescription = topics.get(topicName);
            if (topicDescription == null) {
                logger.warn("Tópico não encontrado: {}", topicName);
                return Collections.emptyList();
            }

            return topicDescription.partitions().stream()
                    .map(TopicPartitionInfo::partition)
                    .toList();

        } catch (Exception e) {
            logger.error("Erro ao descobrir partições do tópico: {}", topicName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Processa todas as partições em paralelo.
     */
    private List<ServiceActivitySnapshot> processPartitions(List<Integer> partitions) {
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(partitions.size(), maxWorkers));

        List<CompletableFuture<List<ServiceActivitySnapshot>>> futures = partitions.stream()
                .map(partitionId -> CompletableFuture.supplyAsync(() -> {
                    logger.info("Worker iniciado para partição {}", partitionId);
                    return worker.processPartition(partitionId, windowDays);
                }, executor))
                .toList();

        // Aguarda conclusão de todos os workers
        List<ServiceActivitySnapshot> allSnapshots = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        executor.shutdown();
        return allSnapshots;
    }
}

