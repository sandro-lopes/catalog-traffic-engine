package com.codingbetter.adapters.github;

import java.util.List;

/**
 * Metadados de um repositório descoberto.
 */
public class RepositoryMetadata {

    private String name;
    private String fullName;
    private String sigla;
    private String type;
    private String serviceName;
    private String serviceId;
    private List<String> tags;
    private String url;
    private String htmlUrl;
    private String description;
    private boolean archived;
    private boolean disabled;
    
    // Campos de ownership
    private String primaryOwner;
    private Double ownershipConfidence;
    private String ownershipSource; // commit-history, dependency-analysis, etc
    
    // Top committers (lista dos top 10)
    private java.util.List<com.codingbetter.ownership.CommitterInfo> topCommitters;

    public RepositoryMetadata() {
    }

    public RepositoryMetadata(String name, String fullName, String sigla, String type, 
                             String serviceName, String serviceId, List<String> tags, String url) {
        this.name = name;
        this.fullName = fullName;
        this.sigla = sigla;
        this.type = type;
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.tags = tags;
        this.url = url;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getPrimaryOwner() {
        return primaryOwner;
    }

    public void setPrimaryOwner(String primaryOwner) {
        this.primaryOwner = primaryOwner;
    }

    public Double getOwnershipConfidence() {
        return ownershipConfidence;
    }

    public void setOwnershipConfidence(Double ownershipConfidence) {
        this.ownershipConfidence = ownershipConfidence;
    }

    public String getOwnershipSource() {
        return ownershipSource;
    }

    public void setOwnershipSource(String ownershipSource) {
        this.ownershipSource = ownershipSource;
    }

    public java.util.List<com.codingbetter.ownership.CommitterInfo> getTopCommitters() {
        return topCommitters;
    }

    public void setTopCommitters(java.util.List<com.codingbetter.ownership.CommitterInfo> topCommitters) {
        this.topCommitters = topCommitters;
    }
}

