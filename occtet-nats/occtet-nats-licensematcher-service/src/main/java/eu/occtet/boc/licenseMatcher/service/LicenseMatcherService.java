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

package eu.occtet.boc.licenseMatcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.licenseMatcher.dao.InventoryItemRepository;
import eu.occtet.boc.licenseMatcher.factory.InventoryItemFactory;
import eu.occtet.boc.licenseMatcher.factory.PromptFactory;
import eu.occtet.boc.licenseMatcher.tools.LicenseMatcher;
import eu.occtet.boc.model.AILicenseMatcherWorkData;
import eu.occtet.boc.model.FossReportServiceWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.NatsStreamSender;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.utility.compare.CompareTemplateOutputHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LicenseMatcherService extends BaseWorkDataProcessor {
    private static final Logger log = LoggerFactory.getLogger(LicenseMatcherService.class);


    @Autowired
    private LicenseMatcher licenseMatcher;

    @Autowired
    private PromptFactory promptFactory;

    @Autowired
    private InventoryItemFactory inventoryItemFactory;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private Connection natsConnection;

    @Value("${nats.send-subject}")
    private String sendSubject;


    @Bean
    public NatsStreamSender natsStreamSender(){
        return new NatsStreamSender(natsConnection, sendSubject);
    }


    @Override
    public boolean process(ScannerSendWorkData workData) {
        log.debug("LicenseMatcherService: matches licenseTexts and calls AI to help {}", workData.toString());
        return generatePrompt(workData);
    }

    /**
     * Generates a prompt for AI processing based on the license information of the given InventoryItem
     * and the result of the spdx matcher
     *
     * @param workData
     * @return
     */
    private boolean generatePrompt(ScannerSendWorkData workData) {
        try {
            long inventoryItemId = workData.getInventoryItemId();

            Optional<InventoryItem> optItem = inventoryItemRepository.findById(inventoryItemId);
            if (optItem.isPresent()) {
                InventoryItem item = optItem.get();
                log.debug("working on item {}, softwareComponent {}", item.getInventoryName(), item.getSoftwareComponent().getName());
                log.debug("softwarecomponent has {} licenses", item.getSoftwareComponent().getLicenses().size());
                for (License license : item.getSoftwareComponent().getLicenses()) {
                    String licenseId = license.getLicenseType();
                    String licenseText = license.getLicenseText();
                    if(licenseId.contains("LicenseRef-") || licenseId.contains("licenseref-")){
                        licenseId= licenseId.replace("LicenseRef-","").replace("licenseref-","");
                    }
                    log.debug("checking inventory item: {}, licenseId: {}", item.getInventoryName(), licenseId);
                    //rule-based, comparing original license text with specific file license text with spdx library
                    CompareTemplateOutputHandler.DifferenceDescription result = licenseMatcher.spdxCompareLicense(licenseId, licenseText);
                    log.debug("checked result : {} isdifference: {}",result, result.isDifferenceFound());
                    if (result != null && result.isDifferenceFound()) {
                        log.debug("license texts are different for licenseId: {}", licenseId);
                        //baseURL for the licenseTool is given to the prompt as parameter, AI is using the tool with it
                        //the result of the spdx matcher is also given for further information
                        String baseURL = "https://raw.githubusercontent.com/spdx/license-list-data/main/json/details/" + licenseId + ".json"; // fixme make configurable later
                        String userMessage = promptFactory.createUserMessage(result);
                        sendAnswerToStream(new AILicenseMatcherWorkData(userMessage, baseURL, result.getDifferenceMessage(), licenseId, licenseText, inventoryItemId));

                    } else if (result == null) {
                        log.debug("result is null");
                        log.error("url not successfully for license: {}", licenseId);
                        if (item.getExternalNotes() == null) {
                            item.setExternalNotes("url not successfully for license: " + licenseId + "/ no spdx match possible");
                        } else {
                            item.setExternalNotes(item.getExternalNotes() + " \n url not successfully for license: " + licenseId + "/ no spdx match possible");
                        }
                    } else {
                        log.debug("license text matched");
                        if (item.getExternalNotes() == null)
                            item.setExternalNotes("License " + licenseId + " matches license text");
                        else {
                            item.setExternalNotes(item.getExternalNotes() + "\n License " + licenseId + " matches license text");
                        }
                    }
                    inventoryItemFactory.update(item);
                    log.debug("updated");
                }
            }
        }catch (Exception e){
            log.error("error in generatePrompt: {}", e.getMessage());
            return false;
        }
        return true;
    }


    /**
     * Sends the AI-generated answer to the NATS stream for further processing.
     * @param aiLicenseMatcherWorkData
     * @throws JetStreamApiException
     * @throws IOException
     */
    private void sendAnswerToStream(AILicenseMatcherWorkData aiLicenseMatcherWorkData) {
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("process_inventoryItems", "sending inventoryItem to next microservice according to config", actualTimestamp, aiLicenseMatcherWorkData);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String message = objectMapper.writeValueAsString(workTask);
            log.debug("sending message to ai service: {} under subject {}", message, natsStreamSender().toString());
            natsStreamSender().sendWorkMessageToStream(message.getBytes(Charset.defaultCharset()));
        }catch(Exception e){
            log.error("error sending message to stream: {}", e.getMessage());
        }
    }

}
