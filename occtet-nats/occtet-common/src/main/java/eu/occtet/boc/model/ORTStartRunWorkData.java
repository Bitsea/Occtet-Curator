package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

public class ORTStartRunWorkData extends BaseWorkData{

    private String repositoryType;
    private String repositoryUrl;
    private String repositoryVersion;
    private String organizationName;
    private long projectId;
    private long runId;

    @JsonCreator
    public ORTStartRunWorkData(@JsonProperty("repositoryType") String repositoryType,
                              @JsonProperty("repositoryUrl") String repositoryUrl,
                              @JsonProperty("repositoryVersion") String repositoryVersion,
                              @JsonProperty("organizationName") String organizationName,
                              @JsonProperty("projectId") long projectId) {
        this.repositoryType = repositoryType;
        this.repositoryUrl = repositoryUrl;
        this.repositoryVersion = repositoryVersion;
        this.organizationName = organizationName;
        this.projectId = projectId;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getRepositoryVersion() {
        return repositoryVersion;
    }

    public void setRepositoryVersion(String repositoryVersion) {
        this.repositoryVersion = repositoryVersion;
    }

    public long getRunId() {
        return runId;
    }

    public void setRunId(long runId) {
        this.runId = runId;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
