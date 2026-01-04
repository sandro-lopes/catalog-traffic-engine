package com.codingbetter.consolidation;

import com.codingbetter.schemas.v1.ServiceActivitySnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class TrafficClassifierTest {

    private final TrafficClassifier classifier = new TrafficClassifier();

    @Test
    void testClassifyActive() {
        Instant lastSeen = Instant.now().minus(5, ChronoUnit.DAYS);
        ServiceActivitySnapshot.Classification result = classifier.classify(lastSeen);
        assertEquals(ServiceActivitySnapshot.Classification.ACTIVE, result);
    }

    @Test
    void testClassifyLowUsage() {
        Instant lastSeen = Instant.now().minus(15, ChronoUnit.DAYS);
        ServiceActivitySnapshot.Classification result = classifier.classify(lastSeen);
        assertEquals(ServiceActivitySnapshot.Classification.LOW_USAGE, result);
    }

    @Test
    void testClassifyNoTraffic() {
        Instant lastSeen = Instant.now().minus(35, ChronoUnit.DAYS);
        ServiceActivitySnapshot.Classification result = classifier.classify(lastSeen);
        assertEquals(ServiceActivitySnapshot.Classification.NO_TRAFFIC, result);
    }

    @Test
    void testClassifyNull() {
        ServiceActivitySnapshot.Classification result = classifier.classify(null);
        assertEquals(ServiceActivitySnapshot.Classification.NO_TRAFFIC, result);
    }
}

