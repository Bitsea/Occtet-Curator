/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

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
        log.info("Received work message from NATS subject '{}' (payload size: {} bytes)", msg.getSubject(), msg.getData() != null ? msg.getData().length : 0);
        String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        WorkTask workTask = null;
        try {
            workTask = objectMapper.readValue(jsonData, WorkTask.class);
            BaseWorkData workData = workTask.workData();
            log.info("Processing WorkTask with workData type: {}", workData != null ? workData.getClass().getSimpleName() : "null");

            boolean result = workData.process(new BaseWorkDataProcessor() {
                @Override
                public boolean process(ORTProcessWorkData workData) {
                    log.info("Dispatching ORTProcessWorkData to ProcessRunService (run ID: {})", workData.getRunId());
                    try {
                        boolean processed = processRunService.process(workData);
                        if (processed) {
                            log.info("Successfully finished processing ORT run ID: {}", workData.getRunId());
                        } else {
                            log.warn("ProcessRunService returned false for ORT run ID: {}", workData.getRunId());
                        }
                        return processed;
                    } catch (Exception e) {
                        log.error("Error occurred while processing ORTProcessWorkData for run ID {}: {}",
                                workData.getRunId(), e.getMessage(), e);
                        return false;
                    }
                }
            });
            if (!result) {
                log.error("Failed to process workData of type {}", workData.getClass().getName());
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize WorkTask JSON: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
