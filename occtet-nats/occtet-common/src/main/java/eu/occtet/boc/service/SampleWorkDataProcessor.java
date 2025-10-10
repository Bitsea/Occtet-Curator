package eu.occtet.boc.service;

import eu.occtet.boc.model.SampleWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;

public class SampleWorkDataProcessor extends BaseWorkDataProcessor {
    @Override
    public boolean process(SampleWorkData workData) {
        System.out.println("Processing SampleWorkData: " + workData.toString());
        return true;
    }

    @Override
    public boolean process(ScannerSendWorkData workData) {
        System.out.println("Processing ScannerSendWorkData: " + workData.toString());
        return true;
    }
}
