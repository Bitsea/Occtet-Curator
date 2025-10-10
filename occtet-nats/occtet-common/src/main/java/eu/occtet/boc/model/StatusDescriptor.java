package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusDescriptor extends BaseSystemMessage {
    private String name;
    private WorkerStatus status;
    private int progressPercent;
    private String details;

    public StatusDescriptor() {
    }

    public StatusDescriptor(String name, WorkerStatus status, int progressPercent, String details) {
        this.name = name;
        this.status = status;
        this.progressPercent = progressPercent;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
