package com.codingbetter.orchestration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicePartitionerTest {

    @Test
    void testPartition() {
        List<String> serviceIds = Arrays.asList("service1", "service2", "service3", "service4", "service5");
        List<List<String>> partitions = ServicePartitioner.partition(serviceIds, 3);

        assertEquals(3, partitions.size());
        assertTrue(partitions.stream().mapToInt(List::size).sum() == 5);
    }

    @Test
    void testGetPartition() {
        String serviceId = "service-123";
        int partition = ServicePartitioner.getPartition(serviceId, 10);
        assertTrue(partition >= 0 && partition < 10);
    }

    @Test
    void testPartitionConsistency() {
        String serviceId = "service-123";
        int partition1 = ServicePartitioner.getPartition(serviceId, 10);
        int partition2 = ServicePartitioner.getPartition(serviceId, 10);
        assertEquals(partition1, partition2); // Mesmo serviço sempre na mesma partição
    }
}

