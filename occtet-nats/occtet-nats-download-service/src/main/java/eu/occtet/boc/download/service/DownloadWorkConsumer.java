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

package eu.occtet.boc.download.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.DownloadServiceWorkData;
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
public class DownloadWorkConsumer extends WorkConsumer {

    private static final Logger log = LoggerFactory.getLogger(DownloadWorkConsumer.class);

    @Autowired
    private DownloadManager downloadManager;

    @Override
    protected void handleMessage(Message msg) {
        log.debug("handleMessage called");

        try {
            String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            WorkTask workTask = objectMapper.readValue(jsonData, WorkTask.class);
            log.debug("workTask: {}", workTask);
            BaseWorkData workData = workTask.workData();
            log.debug("workData: {}", workData);

            boolean result = workData.process(new BaseWorkDataProcessor() {
                @Override
                public boolean process(DownloadServiceWorkData workData) {
                    try {
                        return downloadManager.process(workData);
                    } catch (Exception e) {
                        log.error("Could not process workData of type {} with error message: ", workData.getClass().getName(), e);
                        return false;
                    }
                }
            });

            if (!result) {
                log.error("Could not process workData of type {}", workData.getClass().getName());
            } else {
                log.info("Successfully processed workData of type {}", workData.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
