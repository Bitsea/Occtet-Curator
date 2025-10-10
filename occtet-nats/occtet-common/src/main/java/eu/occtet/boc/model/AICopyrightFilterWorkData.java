package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

import java.util.List;
import java.util.UUID;

public class AICopyrightFilterWorkData extends BaseWorkData {

    private String userMessage;
    private UUID inventoryItemId;
    private List<String> questionableCopyrights;


    public AICopyrightFilterWorkData( UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    @JsonCreator
    public AICopyrightFilterWorkData(@JsonProperty("userMessage")String userMessage,
                                     @JsonProperty("inventoryItemId")UUID inventoryItemId,
                                     @JsonProperty("questionableCopyrights")List<String> questionableCopyrights) {
        this.userMessage = userMessage;
        this.inventoryItemId = inventoryItemId;
        this.questionableCopyrights= questionableCopyrights;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public List<String> getQuestionableCopyrights() {
        return questionableCopyrights;
    }

    public void setQuestionableCopyrights(List<String> questionableCopyrights) {
        this.questionableCopyrights = questionableCopyrights;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }

}
