package com.codingbetter.adapters.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente HTTP para GitHub API com rate limiting e retry.
 * Suporta paginação para lidar com 2k+ repositórios.
 */
@Component
public class GitHubClient {

    private static final Logger logger = LoggerFactory.getLogger(GitHubClient.class);

    private final WebClient webClient;
    private final GitHubConfig config;
    private final ObjectMapper objectMapper;

    public GitHubClient(GitHubConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getApi().getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApi().getToken())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Lista todos os repositórios da organização com paginação.
     * GitHub API retorna no máximo 100 por página.
     */
    @CircuitBreaker(name = "github")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public Flux<JsonNode> listAllRepositories(String organization) {
        logger.info("Listando repositórios da organização: {}", organization);

        return Flux.range(1, Integer.MAX_VALUE)
                .flatMap(page -> fetchRepositoriesPage(organization, page), 1) // Sequencial para respeitar rate limit
                .takeWhile(page -> !page.isEmpty())
                .flatMap(Flux::fromIterable)
                .doOnNext(repo -> logger.debug("Repositório encontrado: {}", repo.get("name").asText()))
                .doOnComplete(() -> logger.info("Listagem de repositórios concluída"));
    }

    /**
     * Busca uma página específica de repositórios.
     */
    private Mono<List<JsonNode>> fetchRepositoriesPage(String organization, int page) {
        logger.debug("Buscando página {} de repositórios", page);

        return webClient.get()
                .uri("/orgs/{org}/repos?per_page=100&page={page}&type=all", organization, page)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> parseRepositoriesPage(json))
                .doOnSuccess(repos -> {
                    if (!repos.isEmpty()) {
                        logger.debug("Página {}: {} repositórios encontrados", page, repos.size());
                    }
                })
                .doOnError(error -> logger.error("Erro ao buscar página {} de repositórios", page, error))
                .onErrorReturn(new ArrayList<>());
    }

    /**
     * Obtém tags de um repositório.
     */
    @CircuitBreaker(name = "github")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public Mono<List<String>> getRepositoryTopics(String organization, String repositoryName) {
        logger.debug("Buscando topics do repositório: {}/{}", organization, repositoryName);

        return webClient.get()
                .uri("/repos/{owner}/{repo}/topics", organization, repositoryName)
                .header(HttpHeaders.ACCEPT, "application/vnd.github.mercy-preview+json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(this::parseTopics)
                .doOnError(error -> logger.error("Erro ao buscar topics do repositório: {}/{}", 
                        organization, repositoryName, error))
                .onErrorReturn(new ArrayList<>());
    }

    private List<JsonNode> parseRepositoriesPage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<JsonNode> repositories = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode repo : root) {
                    repositories.add(repo);
                }
            }

            return repositories;
        } catch (Exception e) {
            logger.error("Erro ao parsear página de repositórios", e);
            return new ArrayList<>();
        }
    }

    private List<String> parseTopics(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode names = root.get("names");
            List<String> topics = new ArrayList<>();

            if (names != null && names.isArray()) {
                for (JsonNode topic : names) {
                    topics.add(topic.asText());
                }
            }

            return topics;
        } catch (Exception e) {
            logger.error("Erro ao parsear topics", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtém commits de um repositório desde uma data específica.
     * Usado para análise de histórico de commits para inferência de ownership.
     */
    @CircuitBreaker(name = "github")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public Mono<List<com.codingbetter.ownership.CommitHistoryStrategy.CommitInfo>> getCommits(
            String fullRepoName, java.time.Instant since) {
        logger.debug("Buscando commits do repositório: {} desde {}", fullRepoName, since);

        String[] parts = fullRepoName.split("/");
        if (parts.length != 2) {
            logger.error("Formato inválido de fullRepoName: {}", fullRepoName);
            return Mono.just(new ArrayList<>());
        }

        String owner = parts[0];
        String repo = parts[1];
        String sinceIso = java.time.format.DateTimeFormatter.ISO_INSTANT.format(since);

        return webClient.get()
                .uri("/repos/{owner}/{repo}/commits?since={since}&per_page=100", owner, repo, sinceIso)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> parseCommits(json))
                .doOnError(error -> logger.error("Erro ao buscar commits do repositório: {}", fullRepoName, error))
                .onErrorReturn(new ArrayList<>());
    }

    private List<com.codingbetter.ownership.CommitHistoryStrategy.CommitInfo> parseCommits(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<com.codingbetter.ownership.CommitHistoryStrategy.CommitInfo> commits = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode commitNode : root) {
                    JsonNode commit = commitNode.get("commit");
                    if (commit != null) {
                        JsonNode author = commit.get("author");
                        JsonNode committer = commitNode.get("author"); // GitHub user info
                        
                        String authorName = null;
                        String authorEmail = null;
                        String authorId = null;
                        String dateStr = null;

                        // Extrai dados do commit.author (informações do commit)
                        if (author != null) {
                            authorName = author.has("name") ? author.get("name").asText() : null;
                            authorEmail = author.has("email") ? author.get("email").asText() : null;
                            dateStr = author.has("date") ? author.get("date").asText() : null;
                        }

                        // Extrai GitHub username/login do commitNode.author (informações do usuário GitHub)
                        if (committer != null && committer.has("login")) {
                            authorId = committer.get("login").asText();
                        } else if (committer != null && committer.has("id")) {
                            // Fallback: usa ID se login não disponível
                            authorId = String.valueOf(committer.get("id").asLong());
                        } else if (authorName != null) {
                            // Fallback: usa nome se não houver login
                            authorId = authorName;
                        }
                        
                        if (dateStr != null && authorId != null) {
                            try {
                                java.time.Instant date = java.time.Instant.parse(dateStr);
                                commits.add(new com.codingbetter.ownership.CommitHistoryStrategy.CommitInfo(
                                        authorId,
                                        authorEmail != null ? authorEmail : "",
                                        authorName != null ? authorName : authorId,
                                        date));
                            } catch (Exception e) {
                                logger.debug("Erro ao parsear data do commit: {}", dateStr);
                            }
                        }
                    }
                }
            }

            return commits;
        } catch (Exception e) {
            logger.error("Erro ao parsear commits", e);
            return new ArrayList<>();
        }
    }

    /**
     * Cria um arquivo em um repositório via GitHub API.
     * Usado para criar catalog-info.yaml.
     */
    @CircuitBreaker(name = "github")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public Mono<String> createFile(String fullRepoName, String path, String content, 
                                   String branch, String commitMessage) {
        logger.debug("Criando arquivo {} no repositório: {} (branch: {})", path, fullRepoName, branch);

        String[] parts = fullRepoName.split("/");
        if (parts.length != 2) {
            logger.error("Formato inválido de fullRepoName: {}", fullRepoName);
            return Mono.error(new IllegalArgumentException("Formato inválido de fullRepoName"));
        }

        String owner = parts[0];
        String repo = parts[1];

        // Base64 encode do conteúdo
        String encodedContent = java.util.Base64.getEncoder().encodeToString(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("message", commitMessage);
        requestBody.put("content", encodedContent);
        requestBody.put("branch", branch);

        return webClient.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> {
                    try {
                        JsonNode root = objectMapper.readTree(json);
                        if (root.has("commit") && root.get("commit").has("sha")) {
                            return root.get("commit").get("sha").asText();
                        }
                        return "unknown";
                    } catch (Exception e) {
                        logger.error("Erro ao parsear resposta de criação de arquivo", e);
                        return "unknown";
                    }
                })
                .doOnSuccess(sha -> logger.debug("Arquivo criado com sucesso: {} (commit: {})", path, sha))
                .doOnError(error -> logger.error("Erro ao criar arquivo: {}", path, error));
    }

    /**
     * Cria um Pull Request em um repositório.
     */
    @CircuitBreaker(name = "github")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public Mono<String> createPullRequest(String fullRepoName, String title, String body, 
                                         String headBranch, String baseBranch) {
        logger.debug("Criando Pull Request no repositório: {} ({} -> {})", fullRepoName, headBranch, baseBranch);

        String[] parts = fullRepoName.split("/");
        if (parts.length != 2) {
            logger.error("Formato inválido de fullRepoName: {}", fullRepoName);
            return Mono.error(new IllegalArgumentException("Formato inválido de fullRepoName"));
        }

        String owner = parts[0];
        String repo = parts[1];

        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("title", title);
        requestBody.put("body", body);
        requestBody.put("head", headBranch);
        requestBody.put("base", baseBranch);

        return webClient.post()
                .uri("/repos/{owner}/{repo}/pulls", owner, repo)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getApi().getTimeoutSeconds()))
                .map(json -> {
                    try {
                        JsonNode root = objectMapper.readTree(json);
                        if (root.has("number")) {
                            String prNumber = root.get("number").asText();
                            String prUrl = root.has("html_url") ? root.get("html_url").asText() : "";
                            logger.info("Pull Request criado: #{} - {}", prNumber, prUrl);
                            return prNumber;
                        }
                        return "unknown";
                    } catch (Exception e) {
                        logger.error("Erro ao parsear resposta de criação de PR", e);
                        return "unknown";
                    }
                })
                .doOnError(error -> logger.error("Erro ao criar Pull Request", error));
    }
}

