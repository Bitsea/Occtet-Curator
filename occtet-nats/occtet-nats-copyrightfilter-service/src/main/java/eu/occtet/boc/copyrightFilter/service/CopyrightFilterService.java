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

package eu.occtet.boc.copyrightFilter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.copyrightFilter.factory.PromptFactory;
import eu.occtet.boc.copyrightFilter.preprocessor.CopyrightPreprocessor;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.model.AICopyrightFilterWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.NatsStreamSender;
import eu.occtet.boc.dao.InventoryItemRepository;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CopyrightFilterService  extends BaseWorkDataProcessor {

    private static final Logger log = LogManager.getLogger(CopyrightFilterService.class);

    @Autowired
    private CopyrightService copyrightService;

    @Autowired
    private CopyrightPreprocessor copyrightPreprocessor;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private PromptFactory promptFactory;

    @Autowired
    private Connection natsConnection;

    @Value("${nats.send-subject}")
    private String sendSubject;


    @Bean
    public NatsStreamSender natsStreamSender() {
        return new NatsStreamSender(natsConnection, sendSubject);
    }


    private static final Path BASEPATH_JSON = Paths.get("src", "main", "resources", "garbage-Copyrights", "garbage-copyrights.json");

    @Override
    public boolean process(ScannerSendWorkData workData) {
        log.debug("CopyrightFilterService: filter false Copyrights with data {}", workData.toString());
        return initializeCopyrightFilter(workData);
    }

    private boolean initializeCopyrightFilter(ScannerSendWorkData scannerSendWorkData) {
        log.debug("inventoryItemId: {}", scannerSendWorkData.getInventoryItemId());
        Optional<InventoryItem> optItem = inventoryItemRepository.findById(scannerSendWorkData.getInventoryItemId());
        if(!optItem.isPresent()) {
            log.warn("InventoryItem with id {} not found", scannerSendWorkData.getInventoryItemId());
            return false;
        }
        InventoryItem item = optItem.get();
        List<String> copyrightTexts = new ArrayList<>();

        if (item.getSoftwareComponent().getCopyrights() != null && !item.getSoftwareComponent().getCopyrights().isEmpty()) {
            List<Copyright> copyrights = item.getSoftwareComponent().getCopyrights();
            //extract the copyright strings from the copyright objects
            for (Copyright copy : copyrights) {
                copyrightTexts.add(copy.getCopyrightText());
            }
            List<String> questionableCopyrights = filterFalsCopyrightsWithGarbageFile(copyrightTexts, item.getSoftwareComponent());
            if (!questionableCopyrights.isEmpty()) {
                log.info("sending copyrightList to ai for inventory item: {}, copyrights to check: {}", item.getInventoryName(), questionableCopyrights.size());
                String message= promptFactory.createFalseCopyrightPrompt();
                AICopyrightFilterWorkData workData = new AICopyrightFilterWorkData( message,item.getId(), questionableCopyrights);
                sendAnswerToStream(workData);
                return true;

            } else return true;
        } else return item.getSoftwareComponent().getCopyrights() == null && item.getSoftwareComponent().getCopyrights().isEmpty();
    }


    public List<String> filterFalsCopyrightsWithGarbageFile(List<String> copyrightTexts, SoftwareComponent item) {
        List<String> garbageCopyrightTexts= copyrightPreprocessor.readGarbageCopyrightsFromJson(BASEPATH_JSON);
        for(String garbage: garbageCopyrightTexts) {
            if(copyrightTexts.contains(garbage)) {
                for(Copyright c: item.getCopyrights()){
                    if(c.getCopyrightText().equals(garbage)) {
                        copyrightService.updateCopyrightAsGarbage(c);
                    }
                }
                copyrightTexts.remove(garbage);
            }
        }
        return copyrightTexts;
    }



    /**
     * Sends the AI-generated answer to the NATS stream for further processing.
     * @param aiCopyrightFilterWorkData
     * @throws JetStreamApiException
     * @throws IOException
     */
    private void sendAnswerToStream(AICopyrightFilterWorkData aiCopyrightFilterWorkData) {
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("process_inventoryItems", "sending inventoryItem to next microservice according to config", actualTimestamp, aiCopyrightFilterWorkData);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String message = objectMapper.writeValueAsString(workTask);
            log.debug("sending message to ai service: {}", message);
            natsStreamSender().sendWorkMessageToStream(message.getBytes(Charset.defaultCharset()));
        }catch(Exception e){
            log.error("error sending message to stream: {}", e.getMessage());
        }
    }
}
