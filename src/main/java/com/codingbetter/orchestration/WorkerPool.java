package com.codingbetter.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Pool de workers para processamento paralelo.
 * Gerencia execução de tarefas distribuídas entre workers.
 */
@Component
public class WorkerPool {

    private static final Logger logger = LoggerFactory.getLogger(WorkerPool.class);

    private final ExecutorService executorService;

    public WorkerPool() {
        // Usa virtual threads do Java 21 para melhor escalabilidade
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Executa tarefas em paralelo usando pool de workers.
     * @param numberOfWorkers Número de workers paralelos
     * @param tasks Lista de tarefas (suppliers) a executar
     * @return Lista de CompletableFutures representando cada tarefa
     */
    public <T> List<CompletableFuture<T>> executeParallel(
            int numberOfWorkers,
            List<Supplier<T>> tasks) {

        logger.info("Executando {} tarefas com {} workers", tasks.size(), numberOfWorkers);

        AtomicInteger completed = new AtomicInteger(0);
        int total = tasks.size();

        return tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> {
                    try {
                        T result = task.get();
                        int count = completed.incrementAndGet();
                        if (count % 10 == 0 || count == total) {
                            logger.info("Progresso: {}/{} tarefas concluídas", count, total);
                        }
                        return result;
                    } catch (Exception e) {
                        logger.error("Erro ao executar tarefa", e);
                        throw new RuntimeException(e);
                    }
                }, executorService))
                .toList();
    }

    /**
     * Aguarda conclusão de todas as tarefas.
     */
    public <T> List<T> waitForCompletion(List<CompletableFuture<T>> futures) {
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

