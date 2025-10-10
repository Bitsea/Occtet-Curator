package eu.occtet.boc.service;

import eu.occtet.boc.model.*;

public abstract class BaseWorkDataProcessor implements IWorkDataProcessor {
    @Override
    public boolean process(AIAnswerWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(AILicenseMatcherWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(FossReportServiceWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(ScannerSendWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(SampleWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(VulnerabilityServiceWorkData workData) {
        return false;
    }

    @Override
    public boolean process(AIStatusQueryWorkData workData) {
        return false;
    }
    @Override
    public boolean process(AICopyrightFilterWorkData workData) {
        return false;
    }

    @Override
    public boolean process(SpdxWorkData workData){return false;}
}
