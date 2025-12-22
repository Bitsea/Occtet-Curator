package eu.occtet.boc.model;

import eu.occtet.boc.service.IWorkDataProcessor;

public class ORTRunWorkData extends BaseWorkData{
    private int runId;

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
