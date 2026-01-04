package com.codingbetter.adapters.dynatrace;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dynatrace")
public class DynatraceConfig {

    private Api api = new Api();
    private Extraction extraction = new Extraction();

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Extraction getExtraction() {
        return extraction;
    }

    public void setExtraction(Extraction extraction) {
        this.extraction = extraction;
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

    public static class Extraction {
        private int batchSize = 50;
        private int rateLimitPerSecond = 10;
        private int serviceDiscoveryCacheTtlMinutes = 60;
        private int maxWorkers = 20;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getRateLimitPerSecond() {
            return rateLimitPerSecond;
        }

        public void setRateLimitPerSecond(int rateLimitPerSecond) {
            this.rateLimitPerSecond = rateLimitPerSecond;
        }

        public int getServiceDiscoveryCacheTtlMinutes() {
            return serviceDiscoveryCacheTtlMinutes;
        }

        public void setServiceDiscoveryCacheTtlMinutes(int serviceDiscoveryCacheTtlMinutes) {
            this.serviceDiscoveryCacheTtlMinutes = serviceDiscoveryCacheTtlMinutes;
        }

        public int getMaxWorkers() {
            return maxWorkers;
        }

        public void setMaxWorkers(int maxWorkers) {
            this.maxWorkers = maxWorkers;
        }
    }
}

