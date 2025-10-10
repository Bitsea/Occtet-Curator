package eu.occtet.boc.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

import java.util.UUID;

public class AILicenseMatcherWorkData extends BaseWorkData{


    private String userMessage;
    private String url;
    private String licenseId;
    private String licenseText;
    private UUID inventoryItemId;
    private String licenseMatcherResult;

    @JsonCreator
    public AILicenseMatcherWorkData(@JsonProperty("userMessage")String userMessage,
                                    @JsonProperty("url")String url,
                                    @JsonProperty("licenseMatcherResult")String licenseMatcherResult,
                                    @JsonProperty("licenseId")String licenseId,
                                    @JsonProperty("licenseText")String licenseText,
                                    @JsonProperty("inventoryItemId")UUID inventoryItemId) {

        this.userMessage= userMessage;
        this.url = url;
        this.licenseMatcherResult = licenseMatcherResult;
        this.licenseId = licenseId;
        this.licenseText = licenseText;
        this.inventoryItemId= inventoryItemId;
    }

    public AILicenseMatcherWorkData(UUID inventoryItemId) {

        this.inventoryItemId= inventoryItemId;
    }

    public AILicenseMatcherWorkData(){}

    public String getLicenseMatcherResult() {
        return licenseMatcherResult;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getLicenseText() {
        return licenseText;
    }

    public void setLicenseText(String licenseText) {
        this.licenseText = licenseText;
    }

    public void setLicenseMatcherResult(String licenseMatcherResult) {
        this.licenseMatcherResult = licenseMatcherResult;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String toString() {
        return "AILicenseMatcherWorkData{userMessage='" + userMessage + "', url='" + url + "'}";
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
