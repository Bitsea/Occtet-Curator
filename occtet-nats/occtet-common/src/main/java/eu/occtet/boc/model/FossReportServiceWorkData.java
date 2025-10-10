package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

import java.util.Map;
import java.util.UUID;

public class FossReportServiceWorkData extends BaseWorkData{

    private UUID scannerInitializerId;
    private Map<String, Object> rowData;

    @JsonCreator
    public FossReportServiceWorkData(
            @JsonProperty("scannerInitializerId") UUID scannerInitializerId,
            @JsonProperty("rowData") Map<String, Object> rowData
    ) {
        this.scannerInitializerId = scannerInitializerId;
        this.rowData = rowData;
    }

    public UUID getScannerInitializerId() {
        return scannerInitializerId;
    }

    public void setScannerInitializerId(UUID scannerInitializerId) {
        this.scannerInitializerId = scannerInitializerId;
    }

    public Map<String, Object> getRowData() {
        return rowData;
    }

    public void setRowData(Map<String, Object> rowData) {
        this.rowData = rowData;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
