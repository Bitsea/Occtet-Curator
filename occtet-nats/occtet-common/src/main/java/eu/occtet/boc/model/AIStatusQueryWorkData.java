package eu.occtet.boc.model;

import eu.occtet.boc.service.IWorkDataProcessor;

public class AIStatusQueryWorkData extends BaseWorkData{
    private String expectedStatus;
    private String details;


    public String getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String toString() {
        return "AIStatusQuery{expectedStatus='" + expectedStatus + "', details='" + details + "'}";
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
