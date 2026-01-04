package com.codingbetter.backstage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backstage")
public class BackstageConfig {

    private Api api = new Api();
    private Integration integration = new Integration();

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Integration getIntegration() {
        return integration;
    }

    public void setIntegration(Integration integration) {
        this.integration = integration;
    }

    public static class Api {
        private String url;
        private String token;
        private int timeoutSeconds = 30;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Integration {
        private int phase = 1;
        private int batchSize = 100;
        private int updateIntervalSeconds = 60;

        public int getPhase() {
            return phase;
        }

        public void setPhase(int phase) {
            this.phase = phase;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getUpdateIntervalSeconds() {
            return updateIntervalSeconds;
        }

        public void setUpdateIntervalSeconds(int updateIntervalSeconds) {
            this.updateIntervalSeconds = updateIntervalSeconds;
        }
    }

    public static class CatalogYaml {
        private boolean enabled = true;
        private String path = ".backstage/catalog-info.yaml";
        private String branchPrefix = "backstage/catalog-info-";
        private int batchSize = 50;
        private String prTitle = "chore: Adicionar catalog-info.yaml para Backstage";
        private String prBody = "Este PR adiciona o arquivo catalog-info.yaml necessário para descoberta no Backstage.\n\nDados gerados automaticamente pelo Catalog Traffic Engine.";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getBranchPrefix() {
            return branchPrefix;
        }

        public void setBranchPrefix(String branchPrefix) {
            this.branchPrefix = branchPrefix;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public String getPrTitle() {
            return prTitle;
        }

        public void setPrTitle(String prTitle) {
            this.prTitle = prTitle;
        }

        public String getPrBody() {
            return prBody;
        }

        public void setPrBody(String prBody) {
            this.prBody = prBody;
        }
    }

    private CatalogYaml catalogYaml = new CatalogYaml();

    public CatalogYaml getCatalogYaml() {
        return catalogYaml;
    }

    public void setCatalogYaml(CatalogYaml catalogYaml) {
        this.catalogYaml = catalogYaml;
    }
}

