package eu.occtet.boc.model;

import eu.occtet.boc.service.IWorkDataProcessor;

/**
 * sample for workData. Add your own subclass of BaseWorkData, add the constructor setting a unique type and add attributes as required.
 */
public class SampleWorkData extends BaseWorkData{
    private String sampleField;



    public String getSampleField() {
        return sampleField;
    }

    public void setSampleField(String sampleField) {
        this.sampleField = sampleField;
    }

    @Override
    public String toString() {
        return "SampleWorkData{" +
                "sampleField='" + sampleField + '\'' +
                '}';
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
