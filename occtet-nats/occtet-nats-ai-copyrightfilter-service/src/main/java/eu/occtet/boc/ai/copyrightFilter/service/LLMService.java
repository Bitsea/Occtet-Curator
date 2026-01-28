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

package eu.occtet.boc.ai.copyrightFilter.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.ai.copyrightFilter.factory.AdvisorFactory;
import eu.occtet.boc.ai.copyrightFilter.factory.PromptFactory;
import eu.occtet.boc.ai.copyrightFilter.postprocessing.PostProcessor;
import eu.occtet.boc.dao.CopyrightRepository;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.model.AIAnswerWorkData;
import eu.occtet.boc.model.AICopyrightFilterWorkData;
import eu.occtet.boc.model.AIStatusQueryWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.NatsStreamSender;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LLMService extends BaseWorkDataProcessor {
    private static final Logger log = LogManager.getLogger(LLMService.class);

    @Autowired
    private PromptFactory promptFactory;

    @Autowired
    private PostProcessor postProcessor;

    @Autowired
    private eu.occtet.boc.dao.InventoryItemRepository inventoryItemRepository;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Autowired
    private AdvisorFactory advisorFactory;

    @Autowired
    @Qualifier("chatClient")
    private ChatClient chatClient;


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
    public boolean process(AICopyrightFilterWorkData workData) {
        log.debug("LLMService: askAI for filtering copyrights {}", workData.toString());
        return filterCopyrightsWithAI(workData);
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
        return  true;
    }


    private boolean filterCopyrightsWithAI(AICopyrightFilterWorkData aiWorkData) {
        log.debug("filterCopyrightsWithAI for inventory item id {}", aiWorkData.getInventoryItemId());
        //MemoryAdvisor is default
        Optional<InventoryItem> optItem = inventoryItemRepository.findById(aiWorkData.getInventoryItemId());
        if(!optItem.isPresent()) {
            log.warn("InventoryItem with id {} not found", aiWorkData.getInventoryItemId());
            return false;
        }
        List<Advisor> advisors = advisorFactory.createAdvisors();
        String response = "";
        List<Copyright> copyrightList= new ArrayList<>();
        String copyrights = createString(aiWorkData.getQuestionableCopyrights(), copyrightList);
        Prompt question = promptFactory.createFalseCopyrightPrompt(copyrights);

        try {
            response = chatClient.prompt(question)
                    .advisors(advisors)
                    .call().content();

        } catch (Exception e) {
            log.error("Exception with calling ai {}", e.getMessage());
        }
        String result = postProcessor.deleteThinking(response);
        log.debug("result of AI: {}", result);
        handleAIResult(optItem.get(), result, copyrightList);
        if(!result.isEmpty()) {
            try {
                sendAnswerToStream(result);
                return true;
            } catch (Exception e) {
                log.error("Error when sending message to stream: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * separate single copyrights with |||,so AI can better discern the single ones
     * @param group
     * @return
     */
    private String createString(List<String> group, List<Copyright> copyrightList){
        StringBuilder b = new StringBuilder();
        for(String c : group){
            copyrightList.add(copyrightRepository.findByCopyrightText(c).getFirst());
            b.append("|||").append(c);
        }
        return b.toString();
    }


    /**
     * Handles the AI result and updates the InventoryItem's external notes accordingly.
     * @param item
     * @param response
     */
    public void handleAIResult(InventoryItem item, String response, List<Copyright> copyrightList){

        if(item.getExternalNotes()== null &&  !response.isEmpty()){
            item.setExternalNotes(response);
        }else{
            item.setExternalNotes(item.getExternalNotes()+"\n"+response);
        }
        for(Copyright c: copyrightList) {
            if (!response.contains(c.getCopyrightText())) {
                c.setGarbage(true);
            }
            c.setAiControlled(true);
            copyrightRepository.save(c);
        }

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
        WorkTask workTask = new WorkTask(UUID.randomUUID().toString(), "question", actualTimestamp, new AIAnswerWorkData(answer));
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(workTask);
        log.debug("sending message to ai service: {}", message);
        natsStreamSender().sendWorkMessageToStream( message.getBytes(Charset.defaultCharset()));
    }
}
