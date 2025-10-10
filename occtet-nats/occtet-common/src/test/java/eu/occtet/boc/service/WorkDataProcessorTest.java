package eu.occtet.boc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;
import junit.framework.TestCase;
import org.junit.Test;

public class WorkDataProcessorTest extends TestCase {

    @Test
    public void testProcessSample() throws JsonProcessingException {
        SampleWorkDataProcessor processor = new SampleWorkDataProcessor();
        // Beispiel JSON-Nachricht für TypeAMessage
        String jsonMessageA = "{\"type\":\"sample\",\"sampleField\":\"WertA\"}";

        ObjectMapper objectMapper = new ObjectMapper();
        BaseWorkData data = objectMapper.readValue(jsonMessageA, BaseWorkData.class);

        boolean res = data.process(processor);

        assertTrue(res);
    }

    public void testProcessSampleScannerWorkData() throws JsonProcessingException {
        SampleWorkDataProcessor processor = new SampleWorkDataProcessor();

        // Beispiel JSON-Nachricht für TypeAMessage
        String jsonMessageA = "{\"type\":\"scannerdata_send\",\"inventoryItemId\":\"2bd05cfd-ebfb-4393-b0eb-faaff27801ab\"}}";

        ObjectMapper objectMapper = new ObjectMapper();
        BaseWorkData data = objectMapper.readValue(jsonMessageA, BaseWorkData.class);

        boolean res = data.process(processor);

        assertTrue(res);
    }
}