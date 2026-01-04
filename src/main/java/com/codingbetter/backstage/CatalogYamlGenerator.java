package com.codingbetter.backstage;

import com.codingbetter.adapters.github.RepositoryMetadata;
import com.codingbetter.ownership.InferredOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Gera arquivo catalog-info.yaml para Backstage.
 * Formato baseado no schema do Backstage Catalog.
 */
@Component
public class CatalogYamlGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CatalogYamlGenerator.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Gera conteúdo do catalog-info.yaml para um repositório.
     */
    public String generateCatalogYaml(RepositoryMetadata repo, InferredOwner owner) {
        StringBuilder yaml = new StringBuilder();

        yaml.append("apiVersion: backstage.io/v1alpha1\n");
        yaml.append("kind: Component\n");
        yaml.append("metadata:\n");
        yaml.append("  name: ").append(repo.getName()).append("\n");
        
        if (repo.getDescription() != null && !repo.getDescription().isEmpty()) {
            yaml.append("  description: ").append(escapeYaml(repo.getDescription())).append(" - Descoberto automaticamente\n");
        } else {
            yaml.append("  description: Serviço descoberto automaticamente pelo Catalog Traffic Engine\n");
        }
        
        yaml.append("  annotations:\n");
        
        if (repo.getHtmlUrl() != null) {
            yaml.append("    github.com/project-slug: ").append(repo.getFullName()).append("\n");
        }
        
        // Anotações de descoberta
        if (repo.getSigla() != null) {
            yaml.append("    governance.discovery.sigla: \"").append(repo.getSigla()).append("\"\n");
        }
        if (repo.getType() != null) {
            yaml.append("    governance.discovery.type: \"").append(repo.getType()).append("\"\n");
        }
        if (repo.getServiceName() != null) {
            yaml.append("    governance.discovery.serviceName: \"").append(repo.getServiceName()).append("\"\n");
        }
        yaml.append("    governance.discovery.discoveredAt: \"").append(ISO_FORMATTER.format(Instant.now())).append("\"\n");
        
        // Anotações de ownership (se disponível)
        if (owner != null && !owner.isUnknown()) {
            yaml.append("    governance.ownership.primaryOwner: \"").append(owner.getOwner()).append("\"\n");
            yaml.append("    governance.ownership.inferredFrom: \"").append(owner.getStrategy()).append("\"\n");
            yaml.append("    governance.ownership.confidence: \"").append(String.format("%.2f", owner.getConfidence())).append("\"\n");
        }
        
        yaml.append("spec:\n");
        yaml.append("  type: service\n");
        yaml.append("  lifecycle: production\n");
        
        // Owner (se disponível)
        if (owner != null && !owner.isUnknown()) {
            yaml.append("  owner: ").append(owner.getOwner()).append("  # Inferido via ").append(owner.getStrategy()).append("\n");
        } else {
            yaml.append("  owner: team-unknown  # Owner não identificado automaticamente\n");
        }

        logger.debug("Catalog YAML gerado para: {}", repo.getName());
        return yaml.toString();
    }

    private String escapeYaml(String text) {
        if (text == null) {
            return "";
        }
        // Escapa caracteres especiais do YAML
        return text.replace("\"", "\\\"")
                  .replace("\n", " ")
                  .replace("\r", "");
    }
}

