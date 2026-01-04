package com.codingbetter.ownership;

import com.codingbetter.adapters.dynatrace.DynatraceClient;
import com.codingbetter.adapters.github.GitHubClient;
import com.codingbetter.adapters.github.RepositoryMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Estratégia de inferência de ownership baseada em histórico de commits.
 * Analisa últimos 6-12 meses de commits para identificar top committers.
 */
@Component
public class CommitHistoryStrategy implements OwnerInferenceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CommitHistoryStrategy.class);

    private final GitHubClient githubClient;

    @Value("${ownership.inference.commit-history.lookback-months:6}")
    private int lookbackMonths;

    @Value("${ownership.inference.commit-history.recent-months:3}")
    private int recentMonths;

    @Value("${ownership.inference.commit-history.recent-weight:2.0}")
    private double recentWeight;

    @Value("${ownership.inference.commit-history.min-commits:5}")
    private int minCommits;

    @Value("${ownership.inference.commit-history.ignore-bots:true}")
    private boolean ignoreBots;

    @Value("${ownership.inference.commit-history.bot-patterns:.*-bot$,.*-ci$,.*-automation$}")
    private List<String> botPatterns;

    private List<Pattern> compiledBotPatterns;

    public CommitHistoryStrategy(GitHubClient githubClient) {
        this.githubClient = githubClient;
        this.compiledBotPatterns = new ArrayList<>();
    }

    @Override
    public InferenceResult infer(RepositoryMetadata repo,
                                 DynatraceClient.EntityDetails entityDetails,
                                 List<String> callers) {
        
        if (repo.getFullName() == null) {
            logger.debug("RepositoryMetadata sem fullName, não é possível analisar commits");
            return null;
        }

        logger.debug("Analisando histórico de commits para: {}", repo.getFullName());

        Instant since = Instant.now().minus(lookbackMonths, ChronoUnit.MONTHS);
        Instant recentThreshold = Instant.now().minus(recentMonths, ChronoUnit.MONTHS);

        // Busca commits via GitHub API
        List<CommitInfo> commits = githubClient.getCommits(repo.getFullName(), since)
                .blockOptional()
                .orElse(new ArrayList<>());

        if (commits == null || commits.isEmpty()) {
            logger.debug("Nenhum commit encontrado para: {}", repo.getFullName());
            return null;
        }

        if (commits.size() < minCommits) {
            logger.debug("Poucos commits ({}) para inferir ownership: {}", commits.size(), repo.getFullName());
            return null;
        }

        // Analisa frequência de commits por autor
        Map<String, Integer> authorFrequency = new HashMap<>();
        Map<String, Integer> recentAuthorFrequency = new HashMap<>();

        for (CommitInfo commit : commits) {
            String authorId = commit.getAuthorId();
            
            // Ignora bots se configurado
            if (ignoreBots && isBot(authorId)) {
                continue;
            }

            authorFrequency.merge(authorId, 1, Integer::sum);

            if (commit.getDate().isAfter(recentThreshold)) {
                recentAuthorFrequency.merge(authorId, (int) recentWeight, Integer::sum);
            }
        }

        if (authorFrequency.isEmpty()) {
            logger.debug("Apenas commits de bots encontrados para: {}", repo.getFullName());
            return null;
        }

        // Combina frequências (recência tem peso maior)
        Map<String, Double> weightedFrequency = new HashMap<>();
        for (String author : authorFrequency.keySet()) {
            double weight = authorFrequency.get(author) + 
                           recentAuthorFrequency.getOrDefault(author, 0);
            weightedFrequency.put(author, weight);
        }

        // Pega autor mais frequente (para inferência de ownership primário)
        String topAuthor = weightedFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (topAuthor == null) {
            return null;
        }

        // Busca email do top author
        String topAuthorEmail = commits.stream()
                .filter(c -> topAuthor.equals(c.getAuthorId()))
                .findFirst()
                .map(CommitInfo::getAuthorEmail)
                .orElse(null);

        // Calcula confiança baseada em:
        // - Percentual de commits do top author
        // - Número total de commits
        double topAuthorPercent = authorFrequency.get(topAuthor) / (double) commits.size();
        double confidence = Math.min(0.85, 0.50 + (topAuthorPercent * 0.35));

        // Se top author tem <30% dos commits, confiança baixa
        if (topAuthorPercent < 0.30) {
            confidence = Math.max(0.50, confidence - 0.20);
        }

        logger.info("Owner inferido via commit history: {} -> {} (confiança: {:.2f})", 
                repo.getFullName(), topAuthor, confidence);

        return new InferenceResult(
                topAuthorEmail != null ? topAuthorEmail : topAuthor, // Usa email se disponível
                null, // Team não inferido por commits
                null, // Area não inferida por commits
                confidence,
                getName()
        );
    }

    private boolean isBot(String author) {
        if (compiledBotPatterns.isEmpty() && botPatterns != null) {
            compiledBotPatterns = botPatterns.stream()
                    .map(Pattern::compile)
                    .collect(Collectors.toList());
        }

        return compiledBotPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(author.toLowerCase()).matches());
    }

    @Override
    public String getName() {
        return "commit-history";
    }

    @Override
    public double getMinConfidence() {
        return 0.60;
    }

    @Override
    public int getPriority() {
        return 1; // Primeira estratégia a tentar
    }

    /**
     * Retorna lista dos top 10 committers com id e email.
     */
    public List<CommitterInfo> getTopCommitters(RepositoryMetadata repo) {
        if (repo.getFullName() == null) {
            return new ArrayList<>();
        }

        Instant since = Instant.now().minus(lookbackMonths, ChronoUnit.MONTHS);
        Instant recentThreshold = Instant.now().minus(recentMonths, ChronoUnit.MONTHS);

        List<CommitInfo> commits = githubClient.getCommits(repo.getFullName(), since)
                .blockOptional()
                .orElse(new ArrayList<>());

        if (commits == null || commits.isEmpty()) {
            return new ArrayList<>();
        }

        // Agrupa commits por committer (id)
        Map<String, CommitterData> committerMap = new HashMap<>();

        for (CommitInfo commit : commits) {
            if (ignoreBots && isBot(commit.getAuthorId())) {
                continue;
            }

            CommitterData data = committerMap.computeIfAbsent(
                    commit.getAuthorId(),
                    id -> new CommitterData(
                            id,
                            commit.getAuthorEmail(),
                            commit.getAuthorName()
                    )
            );

            data.incrementCommits();
            if (commit.getDate().isAfter(recentThreshold)) {
                data.incrementRecentCommits((int) recentWeight);
            }
        }

        // Calcula score ponderado e ordena
        List<CommitterInfo> topCommitters = committerMap.values().stream()
                .map(data -> new CommitterInfo(
                        data.getId(),
                        data.getEmail(),
                        data.getName(),
                        data.getCommitCount(),
                        data.getCommitCount() + data.getRecentCommits()
                ))
                .sorted((a, b) -> Double.compare(b.getWeightedScore(), a.getWeightedScore()))
                .limit(10)
                .collect(Collectors.toList());

        return topCommitters;
    }

    /**
     * Classe auxiliar para representar informações de commit.
     */
    public static class CommitInfo {
        private final String authorId; // GitHub username/login
        private final String authorEmail;
        private final String authorName;
        private final Instant date;

        public CommitInfo(String authorId, String authorEmail, String authorName, Instant date) {
            this.authorId = authorId;
            this.authorEmail = authorEmail;
            this.authorName = authorName;
            this.date = date;
        }

        public String getAuthorId() {
            return authorId;
        }

        public String getAuthorEmail() {
            return authorEmail;
        }

        public String getAuthorName() {
            return authorName;
        }

        public Instant getDate() {
            return date;
        }

        // Método de compatibilidade (mantido para inferência de ownership)
        @Deprecated
        public String getAuthor() {
            return authorId;
        }
    }

    /**
     * Classe auxiliar para acumular dados de committer.
     */
    private static class CommitterData {
        private final String id;
        private final String email;
        private final String name;
        private int commitCount = 0;
        private int recentCommits = 0;

        public CommitterData(String id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }

        public void incrementCommits() {
            commitCount++;
        }

        public void incrementRecentCommits(int weight) {
            recentCommits += weight;
        }

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public int getCommitCount() {
            return commitCount;
        }

        public int getRecentCommits() {
            return recentCommits;
        }
    }
}

