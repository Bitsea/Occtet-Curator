/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.WorkConsumer;
import io.nats.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class FileSearchServiceWorkConsumer extends WorkConsumer {

    private static final Logger log = LoggerFactory.getLogger(FileSearchServiceWorkConsumer.class);

    @Autowired
    private FileSearchService fileSearchService;

    @Override
    protected void handleMessage(Message msg) {
        log.debug("handleMessage called");
        String JsonData = new String(msg.getData(), StandardCharsets.UTF_8);
        log.debug("JsonData: {}", JsonData);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            WorkTask workTask = objectMapper.readValue(JsonData, WorkTask.class);
            log.debug("workTask: {}", workTask);
            BaseWorkData workData = workTask.workData();
            log.debug("workData: {}", workData);
            // add service calls and so here if needed to process the work data (currently no use) ...
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
