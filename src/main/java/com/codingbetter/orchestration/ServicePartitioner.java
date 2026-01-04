package com.codingbetter.orchestration;

import java.util.ArrayList;
import java.util.List;

/**
 * Particionador de serviços para distribuição entre workers.
 * Garante distribuição uniforme usando hash do service.id.
 */
public class ServicePartitioner {

    /**
     * Particiona lista de serviços em N partições.
     * Usa hash do service.id para garantir distribuição uniforme.
     */
    public static List<List<String>> partition(List<String> serviceIds, int numberOfPartitions) {
        if (numberOfPartitions <= 0) {
            throw new IllegalArgumentException("Número de partições deve ser > 0");
        }

        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < numberOfPartitions; i++) {
            partitions.add(new ArrayList<>());
        }

        for (String serviceId : serviceIds) {
            int partition = Math.abs(serviceId.hashCode()) % numberOfPartitions;
            partitions.get(partition).add(serviceId);
        }

        return partitions;
    }

    /**
     * Calcula partição para um service.id específico.
     * Útil para garantir que o mesmo serviço sempre vai para a mesma partição.
     */
    public static int getPartition(String serviceId, int numberOfPartitions) {
        return Math.abs(serviceId.hashCode()) % numberOfPartitions;
    }
}

