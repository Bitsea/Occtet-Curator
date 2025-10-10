package eu.occtet.boc.fossreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.FossReportServiceWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.WorkConsumer;
import io.nats.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class FossReportWorkConsumer extends WorkConsumer {

    private static final Logger log = LoggerFactory.getLogger(FossReportWorkConsumer.class);


    @Autowired
    private FossReportService fossReportService;

    @Override
    protected void handleMessage(Message msg) {
        log.debug("handleMessage called");
        String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        WorkTask workTask = null;
        try {
            workTask = objectMapper.readValue(jsonData,WorkTask.class);
            log.debug("workTask: {}", workTask);
            BaseWorkData workData = workTask.workData();
            log.debug("workData: {}", workData);
            boolean result = workData.process(new BaseWorkDataProcessor() {
               @Override
               public boolean process(FossReportServiceWorkData workData) {
                   try {
                       return fossReportService.process(workData);
                   } catch (Exception e) {
                       log.error("Could not process workData of type {} with error message: ",
                               workData.getClass().getName(), e);
                       return false;
                   }
               }
            });
            if (!result) {
                log.error("Could not process workData of type {}", workData.getClass().getName());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
