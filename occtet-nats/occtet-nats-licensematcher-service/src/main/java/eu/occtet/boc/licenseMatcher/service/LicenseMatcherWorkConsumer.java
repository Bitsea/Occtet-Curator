package eu.occtet.boc.licenseMatcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.*;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.WorkConsumer;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class LicenseMatcherWorkConsumer extends WorkConsumer {

    private static final Logger log = LoggerFactory.getLogger(LicenseMatcherWorkConsumer.class);

    @Autowired
    private LicenseMatcherService licenseMatcherService;



    @Override
    protected void handleMessage(Message msg){
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
            boolean result = workData.process(new BaseWorkDataProcessor() {
                @Override
                public boolean process(ScannerSendWorkData fossData) {
                    try {
                        log.debug("go to generate");
                        return licenseMatcherService.process(fossData);
                    }catch(Exception e){
                            log.error("Error processing ScannerSendWorkData", e);
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
