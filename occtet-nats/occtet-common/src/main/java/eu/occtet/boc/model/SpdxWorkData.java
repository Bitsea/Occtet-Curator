package eu.occtet.boc.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

public class SpdxWorkData extends BaseWorkData{

    @JsonCreator
    public SpdxWorkData(@JsonProperty("jsonSpdx")byte[] jsonSpdx,
                        @JsonProperty("projectId")String projectId,
                        @JsonProperty("rootInventoryItemId")String rootInventoryItemId,
                        @JsonProperty("useCopyrightAi")boolean useCopyrightAi,
                        @JsonProperty("useLicenseMatcher")boolean useLicenseMatcher) {
        this.jsonSpdx=jsonSpdx;
        this.projectId = projectId;
        this.rootInventoryItemId = rootInventoryItemId;
        this.useCopyrightAi = useCopyrightAi;
        this.useLicenseMatcher = useLicenseMatcher;
    }

    public SpdxWorkData() { }

    private byte[] jsonSpdx;

    private String projectId;

    private String rootInventoryItemId;

    boolean useCopyrightAi;

    boolean useLicenseMatcher;

    public byte[] getJsonSpdx() {
        return jsonSpdx;
    }

    public void setJsonSpdx(byte[] jsonSpdx) {
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

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
