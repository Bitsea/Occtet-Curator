package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

public class ORTProcessWorkData extends BaseWorkData{

    private long runId;

    private long projectId;

    @JsonCreator
    public ORTProcessWorkData(@JsonProperty("runId") long runId,
                              @JsonProperty("projectId") long projectId){
        this.runId= runId;
        this.projectId= projectId;
    }

    public long getRunId() {
        return runId;
    }

    public void setRunId(long runId) {
        this.runId = runId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return false;
    }
}
