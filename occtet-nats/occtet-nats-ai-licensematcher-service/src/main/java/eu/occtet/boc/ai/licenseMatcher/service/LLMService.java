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

package eu.occtet.boc.ai.licenseMatcher.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.ai.licenseMatcher.dao.InventoryItemRepository;
import eu.occtet.boc.ai.licenseMatcher.dao.SoftwareComponentRepository;
import eu.occtet.boc.ai.licenseMatcher.factory.PromptFactory;
import eu.occtet.boc.ai.licenseMatcher.postprocessing.PostProcessor;
import eu.occtet.boc.ai.licenseMatcher.tools.LicenseTool;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.model.*;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.NatsStreamSender;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class LLMService extends BaseWorkDataProcessor {
    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    @Autowired
    private PromptFactory promptFactory;

    @Autowired
    private PostProcessor postProcessor;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;


    @Autowired
    @Qualifier("chatClient")
    private ChatClient chatClient;

    @Autowired
    private LicenseTool licenseTool;

    @Autowired
    private Connection natsConnection;

    @Value("${nats.send-subject}")
    private String sendSubject;


    @Bean
    public NatsStreamSender natsStreamSender(){
        return new NatsStreamSender(natsConnection, sendSubject);
    }

    @Override
    public boolean process(AIStatusQueryWorkData workData) {
        log.debug("LLMService: askAI for StatusQuery {}", workData.toString());
        return getAIStatus(workData);
    }

    @Override
    public boolean process(AILicenseMatcherWorkData workData) {
        log.debug("LLMService: askAI for license matching {}", workData.toString());
        return licenseMatchingAI(workData);
    }


    private boolean getAIStatus(AIStatusQueryWorkData baseWorkData) {
        //MemoryAdvisor is default
        String response = "";
        try {
            response = chatClient.prompt(baseWorkData.getDetails())
                    .call().content();

        } catch (Exception e) {
            log.error("Exception with calling ai {}", e.getMessage());
            return false;
        }

        postProcessor.deleteThinking(response);

        return  response!=null;
    }



    private boolean licenseMatchingAI(AILicenseMatcherWorkData aiWorkData) {
        //MemoryAdvisor is default
        Optional<InventoryItem> optItem = inventoryItemRepository.findById(aiWorkData.getInventoryItemId());
        if(!optItem.isPresent()) {
            log.warn("InventoryItem with id {} not found", aiWorkData.getInventoryItemId());
            return false;
        }

        String response = "";
        Prompt question = promptFactory.createLicenseMatcherPrompt( aiWorkData.getUrl(), aiWorkData.getLicenseText(), aiWorkData.getLicenseMatcherResult(), aiWorkData.getDifferenceLines());
        try {
            response = chatClient.prompt(question)
                    .tools(licenseTool)
                    .call().content();

        } catch (Exception e) {
            log.error("Exception with calling ai {}", e.getMessage());
        }
        String result = postProcessor.deleteThinking(response);
        handleAIResult(optItem.get(), result);
        if(!result.isEmpty()) {
            try {
                sendAnswerToStream(result);
            } catch (Exception e) {
                log.error("Error when sending message to stream: {}", e.getMessage());
                return false;
            }
        }
        return true;
    }


    /**
     * Handles the AI result and updates the InventoryItem's external notes accordingly.
     * @param item
     * @param response
     */
    public void handleAIResult(InventoryItem item, String response){

        if(item.getExternalNotes()== null &&  !response.isEmpty()){
            item.setExternalNotes(response);
        }else{
            item.setExternalNotes(item.getExternalNotes()+"\n"+response);
        }
        item.getSoftwareComponent().setLicenseAiControlled(true);
        softwareComponentRepository.save(item.getSoftwareComponent());
        inventoryItemRepository.save(item);
    }

    /**
     * Sends the AI-generated answer to the NATS stream for further processing.
     * @param answer
     * @throws JetStreamApiException
     * @throws IOException
     */
    private void sendAnswerToStream(String answer) throws JetStreamApiException, IOException {

        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("status_request", "question", actualTimestamp, new AIAnswerWorkData(answer));
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(workTask);

        log.debug("sending message to ai service: {}", message);
        natsStreamSender().sendWorkMessageToStream( message.getBytes(Charset.defaultCharset()));
    }
}
