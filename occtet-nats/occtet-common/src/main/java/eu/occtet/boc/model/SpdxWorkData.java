package eu.occtet.boc.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

public class SpdxWorkData extends BaseWorkData{

    @JsonCreator
    public SpdxWorkData(@JsonProperty("jsonSpdx")String jsonSpdx,
                        @JsonProperty("bucketName")String bucketName,
                        @JsonProperty("projectId")String projectId,
                        @JsonProperty("rootInventoryItemId")String rootInventoryItemId,
                        @JsonProperty("useCopyrightAi")boolean useCopyrightAi,
                        @JsonProperty("useLicenseMatcher")boolean useLicenseMatcher) {
        this.jsonSpdx=jsonSpdx;
        this.bucketName=bucketName;
    }

    public SpdxWorkData() { }

    private String jsonSpdx;

    private String bucketName;

    private String projectId;

    private String rootInventoryItemId;

    boolean useCopyrightAi;

    boolean useLicenseMatcher;

    public String getJsonSpdx() {
        return jsonSpdx;
    }

    public void setJsonSpdx(String jsonSpdx) {
        this.jsonSpdx = jsonSpdx;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRootInventoryItemId() {
        return rootInventoryItemId;
    }

    public void setRootInventoryItemId(String rootInventoryItemId) {
        this.rootInventoryItemId = rootInventoryItemId;
    }

    public boolean isUseCopyrightAi() {
        return useCopyrightAi;
    }

    public void setUseCopyrightAi(boolean useCopyrightAi) {
        this.useCopyrightAi = useCopyrightAi;
    }

    public boolean isUseLicenseMatcher() {
        return useLicenseMatcher;
    }

    public void setUseLicenseMatcher(boolean useLicenseMatcher) {
        this.useLicenseMatcher = useLicenseMatcher;
    }

    public String getBucketName() {return bucketName;}

    public void setBucketName(String bucketName) {this.bucketName = bucketName;}

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
