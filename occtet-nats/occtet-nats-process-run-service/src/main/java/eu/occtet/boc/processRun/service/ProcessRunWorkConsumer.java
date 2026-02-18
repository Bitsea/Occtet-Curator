package eu.occtet.boc.processRun.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.WorkConsumer;
import io.nats.client.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ProcessRunWorkConsumer extends WorkConsumer {

    private static final Logger log = LogManager.getLogger(ProcessRunWorkConsumer.class);

    @Autowired
    private ProcessRunService processRunService;


    protected void handleMessage(Message msg) {
        // actually do work here and update the progressPercent attribute accordingly
        log.debug("handleMessage called");
        log.debug("sending message to issue catcher service: {}", msg);
        String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        WorkTask workTask = null;
        try {
            workTask = objectMapper.readValue(jsonData, WorkTask.class);
            log.debug("workTask: {}", workTask);
            BaseWorkData workData = workTask.workData();
            boolean result = workData.process(new BaseWorkDataProcessor() {
                @Override
                public boolean process(ORTProcessWorkData workData) {
                    log.debug("workData: {}", workData.toString());
                    try {
                        return processRunService.process(workData);
                    } catch (Exception e) {
                        log.error("Could not process workData of type {} with error message: ",
                                workData.getClass().getName(), e);
                        return false;
                    }
                }
            });
            if(!result){
                log.error("Could not process workData of type {}", workData.getClass().getName());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
