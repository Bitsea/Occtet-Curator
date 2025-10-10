package eu.occtet.boc.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

import java.util.Map;
import java.util.UUID;

public class ScannerSendWorkData extends BaseWorkData{

    private UUID inventoryItemId;

    public ScannerSendWorkData() {
    }
    @JsonCreator
    public ScannerSendWorkData(@JsonProperty("inventoryItemId") UUID inventoryItemId) {

        this.inventoryItemId = inventoryItemId;
    }


    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {}

    public String toString(){
        return "ScannerSendWorkData - inventoryItemId: "+ inventoryItemId;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
