package com.codingbetter.orchestration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wrapper para gerenciamento de circuit breakers.
 * Facilita monitoramento e controle de circuit breakers em toda a aplicação.
 */
@Component
public class CircuitBreakerManager {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerManager.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerManager(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Obtém estado atual de um circuit breaker.
     */
    public io.github.resilience4j.circuitbreaker.CircuitBreaker.State getState(String name) {
        return circuitBreakerRegistry.circuitBreaker(name).getState();
    }

    /**
     * Reseta um circuit breaker manualmente.
     */
    public void reset(String name) {
        circuitBreakerRegistry.circuitBreaker(name).reset();
        logger.info("Circuit breaker resetado: {}", name);
    }

    /**
     * Retorna métricas de um circuit breaker.
     */
    public io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics getMetrics(String name) {
        return circuitBreakerRegistry.circuitBreaker(name).getMetrics();
    }
}

