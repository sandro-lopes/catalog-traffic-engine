package com.codingbetter.adapters.github;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "github")
public class GitHubConfig {

    private Api api = new Api();
    private Discovery discovery = new Discovery();

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Discovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
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

    public static class Discovery {
        private List<String> siglas;
        private List<String> types;
        private List<String> tags;
        private int cacheTtlMinutes = 240;

        public List<String> getSiglas() {
            return siglas;
        }

        public void setSiglas(List<String> siglas) {
            this.siglas = siglas;
        }

        public List<String> getTypes() {
            return types;
        }

        public void setTypes(List<String> types) {
            this.types = types;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public int getCacheTtlMinutes() {
            return cacheTtlMinutes;
        }

        public void setCacheTtlMinutes(int cacheTtlMinutes) {
            this.cacheTtlMinutes = cacheTtlMinutes;
        }
    }
}

