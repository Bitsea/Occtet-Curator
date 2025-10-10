package eu.occtet.boc.service;

import eu.occtet.boc.model.*;

public interface IWorkDataProcessor {

    boolean process(AIAnswerWorkData workData);
    boolean process(AILicenseMatcherWorkData workData);
    boolean process(FossReportServiceWorkData workData);
    boolean process(ScannerSendWorkData workData);
    boolean process(SampleWorkData workData);
    boolean process(AIStatusQueryWorkData workData);
    boolean process(VulnerabilityServiceWorkData workData);
    boolean process(SpdxWorkData workData);
    boolean process(AICopyrightFilterWorkData workData);

}
