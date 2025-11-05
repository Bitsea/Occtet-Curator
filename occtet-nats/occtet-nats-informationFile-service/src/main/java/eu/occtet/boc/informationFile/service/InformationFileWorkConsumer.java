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
package eu.occtet.boc.informationFile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.InformationFileSendWorkData;
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
public class InformationFileWorkConsumer extends WorkConsumer {


    private static final Logger log = LogManager.getLogger(InformationFileWorkConsumer.class);

    @Autowired
    private InformationFileService informationFileService;

    @Override
    protected void handleMessage(Message msg){

        log.debug("handleMessage called");
        String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        WorkTask workTask = null;
        try {
            workTask = objectMapper.readValue(jsonData, WorkTask.class);
            log.debug("workTask: {}", workTask);
            BaseWorkData workData = workTask.workData();
            log.debug("workData: {}", workData);

            boolean result = workData.process(new BaseWorkDataProcessor() {

                @Override
                public boolean process(InformationFileSendWorkData workData) {
                    try {
                        log.debug("go to upload files");
                        return informationFileService.uploadFiles(workData.getPath(),"-");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
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
