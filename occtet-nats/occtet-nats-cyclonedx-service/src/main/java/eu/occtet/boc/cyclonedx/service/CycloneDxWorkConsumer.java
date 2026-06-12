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

package eu.occtet.boc.cyclonedx.service;


import eu.occtet.boc.cyclonedx.exception.SpdxImportException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.*;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.WorkConsumer;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.ObjectStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class CycloneDxWorkConsumer extends WorkConsumer {

    private static final Logger log = LogManager.getLogger(CycloneDxWorkConsumer.class);

    @Autowired
    private Connection natsConnection;

    @Autowired
    private CycloneDxService cycloneDxService;

    @Override
    protected void handleMessage(Message msg) {
        try {


            String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("handleMessage called json: {}", jsonData);
            ObjectMapper objectMapper = new ObjectMapper();
            WorkTask workTask = objectMapper.readValue(jsonData, WorkTask.class);
            log.debug("workTask: {}", workTask);
            BaseWorkData workData = workTask.workData();
            log.debug("workData: {}", workTask);

            boolean result = workData.process(new BaseWorkDataProcessor() {
                @Override
                public boolean process(CycloneDxWorkData cycloneDxWorkData) {
                    log.debug("extract from CycloneDx json");
                    try{
                        ObjectStore objectStore = natsConnection.objectStore(cycloneDxWorkData.getBucketName());
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        objectStore.get(cycloneDxWorkData.getJsonSpdx(), out);
                        byte[] spdxBytes = out.toByteArray();
                        //delete the object after we are done
                        objectStore.delete(cycloneDxWorkData.getJsonSpdx());
                        cycloneDxWorkData.setJsonBytes(spdxBytes);
                        cycloneDxService.setOnProgress((p, d)->{
                            log.debug("progress callback: {} {}", p, d);
                            notifyProgress(workTask.taskId(), workTask.name(), WorkTaskStatus.IN_PROGRESS, p, d);
                        });
                        boolean res= cycloneDxService.process(cycloneDxWorkData);
                        if(!res) notifyError(workTask.taskId(),workTask.name(), "error during processing");
                        else notifyCompleted(workTask.taskId(),workTask.name());
                        return res;


                    } catch (Exception e) {
                        log.error("System error processing SPDX", e);
                        notifyError(workTask.taskId(), workTask.name(), "Internal System Error");
                        return false;
                    }
                }
            });
            log.debug("RESULT {}", result);
            if(!result){
                log.error("Could not process workData of type {}", workData.getClass().getName());
            }
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }


    }
}
