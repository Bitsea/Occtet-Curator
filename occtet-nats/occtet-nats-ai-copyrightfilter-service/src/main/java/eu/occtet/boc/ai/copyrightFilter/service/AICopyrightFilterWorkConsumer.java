package eu.occtet.boc.ai.copyrightFilter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.WorkConsumer;
import io.nats.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AICopyrightFilterWorkConsumer extends WorkConsumer {

    private static final Logger log = LogManager.getLogger(AICopyrightFilterWorkConsumer.class);

    @Autowired
    private LLMService llmService;


    protected void handleMessage(Message msg) {
        // actually do work here and update the progressPercent attribute accordingly
        log.debug("handleMessage called");
        String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        WorkTask workTask = null;
        try {
            workTask = objectMapper.readValue(jsonData, WorkTask.class);
            log.debug("workTask: {}", workTask);
            BaseWorkData workData = workTask.workData();
            log.debug("workData: {}", workTask);
            boolean result = workData.process(llmService);
            if(!result){
                log.error("Could not process workData of type {}", workData.getClass().getName());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
