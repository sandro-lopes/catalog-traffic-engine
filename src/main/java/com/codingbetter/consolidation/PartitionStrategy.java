package com.codingbetter.consolidation;

import org.springframework.stereotype.Component;

/**
 * Estratégia de particionamento para consolidação.
 * Define como partições são atribuídas a workers.
 */
@Component
public class PartitionStrategy {

    /**
     * Calcula número ideal de workers baseado no número de partições.
     */
    public int calculateOptimalWorkers(int numberOfPartitions, int minWorkers, int maxWorkers) {
        // Idealmente, um worker por partição
        int optimal = numberOfPartitions;

        // Respeita limites configurados
        return Math.max(minWorkers, Math.min(optimal, maxWorkers));
    }

    /**
     * Distribui partições entre workers (round-robin).
     */
    public java.util.Map<Integer, java.util.List<Integer>> distributePartitions(
            java.util.List<Integer> partitions,
            int numberOfWorkers) {

        java.util.Map<Integer, java.util.List<Integer>> distribution = new java.util.HashMap<>();

        for (int i = 0; i < partitions.size(); i++) {
            int workerId = i % numberOfWorkers;
            distribution.computeIfAbsent(workerId, k -> new java.util.ArrayList<>())
                    .add(partitions.get(i));
        }

        return distribution;
    }
}

