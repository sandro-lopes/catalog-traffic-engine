package com.codingbetter.adapters.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser para extrair sigla, tipo e nome do padrão de nomenclatura: {sigla}-{tipo}-{nome}
 */
@Component
public class RepositoryNameParser {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryNameParser.class);
    
    // Padrão: {sigla}-{tipo}-{nome}
    // Exemplos: abc-api-usuario, xyz-bff-pedidos, abc-gtw-gateway
    private static final Pattern REPO_NAME_PATTERN = Pattern.compile("^([a-z0-9]+)-([a-z]+)-(.+)$");

    /**
     * Parse do nome do repositório para extrair sigla, tipo e nome do serviço.
     * @param repositoryName Nome do repositório (ex: "abc-api-usuario")
     * @return ParsedRepository ou null se não seguir o padrão
     */
    public ParsedRepository parse(String repositoryName) {
        if (repositoryName == null || repositoryName.isEmpty()) {
            return null;
        }

        Matcher matcher = REPO_NAME_PATTERN.matcher(repositoryName.toLowerCase());
        
        if (matcher.matches()) {
            String sigla = matcher.group(1);
            String type = matcher.group(2);
            String serviceName = matcher.group(3);
            
            logger.debug("Repositório parseado: {} -> sigla={}, type={}, serviceName={}", 
                    repositoryName, sigla, type, serviceName);
            
            return new ParsedRepository(sigla, type, serviceName);
        }

        logger.debug("Repositório não segue o padrão: {}", repositoryName);
        return null;
    }

    /**
     * Verifica se o repositório segue o padrão esperado.
     */
    public boolean matchesPattern(String repositoryName) {
        return repositoryName != null && REPO_NAME_PATTERN.matcher(repositoryName.toLowerCase()).matches();
    }

    public static class ParsedRepository {
        private final String sigla;
        private final String type;
        private final String serviceName;

        public ParsedRepository(String sigla, String type, String serviceName) {
            this.sigla = sigla;
            this.type = type;
            this.serviceName = serviceName;
        }

        public String getSigla() {
            return sigla;
        }

        public String getType() {
            return type;
        }

        public String getServiceName() {
            return serviceName;
        }
    }
}

