/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.spdx.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.model.WorkTask;
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
public class SpdxWorkConsumer extends WorkConsumer {

    private static final Logger log = LogManager.getLogger(SpdxWorkConsumer.class);

    @Autowired
    private Connection natsConnection;

    @Autowired
    private SpdxService spdxService;

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
                public boolean process(SpdxWorkData spdxWorkData) {
                    log.debug("extract from spdx json");
                    try{
                        ObjectStore objectStore = natsConnection.objectStore(spdxWorkData.getBucketName());
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        objectStore.get(spdxWorkData.getJsonSpdx(), out);
                        byte[] spdxBytes = out.toByteArray();
                        //delete the object after we are done
                        objectStore.delete(spdxWorkData.getJsonSpdx());
                        spdxWorkData.setJsonBytes(spdxBytes);
                        spdxService.setOnProgress((p,d)->{
                            log.debug("progress callback: {} {}", p, d);
                            notifyProgress(workTask.taskId(), p, d);
                        });
                        return spdxService.process(spdxWorkData);

                    } catch (java.io.IOException | io.nats.client.JetStreamApiException | java.lang.InterruptedException |
                             java.security.NoSuchAlgorithmException e) {
                       log.error("failed to get json from objectStore: {}", e.toString());
                        return false;
                    }
                }
            });
            log.debug("RESULT {}", result);
            if(!result){
                log.error("Could not process workData of type {}", workData.getClass().getName());
            }
        }
        catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }


    }
}
