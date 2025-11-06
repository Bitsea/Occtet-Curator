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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.model.DownloadServiceWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;
import eu.occtet.boc.model.VulnerabilityServiceWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.NatsStreamSender;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class AnswerService {

    private static final Logger log = LogManager.getLogger(AnswerService.class);

    @Autowired
    private Connection natsConnection;

    @Value("${nats.send-subject1}")
    private String sendSubject1;
    @Value("${nats.send-subject2}")
    private String sendSubject2;
    @Value("${nats.send-subject3}")
    private String sendSubject3;
    @Value("${nats.send-subject4}")
    private String sendSubject4;

    @Bean
    public NatsStreamSender natsStreamSenderLicenseMatcher(){
        return new NatsStreamSender(natsConnection, sendSubject1);
    }

    @Bean
    public NatsStreamSender natsStreamSenderCopyrightFilter(){
        return new NatsStreamSender(natsConnection, sendSubject2);
    }

    @Bean
    public NatsStreamSender natsStreamSenderVulnerabilities(){
        return new NatsStreamSender(natsConnection, sendSubject3);
    }

    @Bean
    public NatsStreamSender natsStreamSenderDownloads(){
        return new NatsStreamSender(natsConnection, sendSubject4);
    }


    /**
     * Sends answer about entities to the NATS stream for further processing.
     * @param inventoryItems list of entities to be included in messages
     * @param toCopyrightAi weather to send to copyright microservice
     * @param toLicenseMatcher weather to send to copyright microservice
     * @return true if sending was successful otherwise false
     * @throws JetStreamApiException
     * @throws IOException
     */
    public boolean prepareAnswers(List<InventoryItem> inventoryItems, boolean toCopyrightAi, boolean toLicenseMatcher) throws JetStreamApiException, IOException {
            log.debug("prepare answer size {}", inventoryItems.size());
        for (InventoryItem inventoryItem : inventoryItems) {
            log.debug("SEND inventoryId {}", inventoryItem.getId());
            ScannerSendWorkData sendWorkData = new ScannerSendWorkData(inventoryItem.getId());
            LocalDateTime now = LocalDateTime.now();
            long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            log.debug("scannerWorkData: {}", sendWorkData.toString());
            if (toCopyrightAi) {
                WorkTask workTask = new WorkTask("copyrightFilter_task", "send processed inventory item from spdx microservice to copyrightFilter", actualTimestamp, sendWorkData);
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                String message = mapper.writeValueAsString(workTask);
                log.debug("sending message to copyrightfilter service: {}", message);
                natsStreamSenderCopyrightFilter().sendWorkMessageToStream(message.getBytes(Charset.defaultCharset()));
            }
            if (toLicenseMatcher) {
                WorkTask workTask = new WorkTask("licenseMatcher_task", "send processed inventory item from spdx microservice to licenseMatcher", actualTimestamp, sendWorkData);
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                String message = mapper.writeValueAsString(workTask);
                log.debug("sending message to licenseMatcher service: {}", message);
                natsStreamSenderLicenseMatcher().sendWorkMessageToStream(message.getBytes(Charset.defaultCharset()));
            }

            VulnerabilityServiceWorkData vulnerabilityWorkData= new VulnerabilityServiceWorkData(inventoryItem.getSoftwareComponent().getId());
            WorkTask workTask = new WorkTask("vulnerability_task", "send processed softwareComponent from spdx microservice to vulnerabilityService", actualTimestamp, vulnerabilityWorkData);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            String message = mapper.writeValueAsString(workTask);
            log.debug("sending message to vulnerability service: {}", message);
            natsStreamSenderVulnerabilities().sendWorkMessageToStream(message.getBytes(Charset.defaultCharset()));
        }
        return true;
    }

    /**
     * Send messages to the DownloadService, the service downloads the component at the specified link with the provided version to the base path
     * @param url url where the component is located
     * @param location location path where the component will be downloaded into
     * @param version version of the component to be downloaded
     * @return true if sending was successful otherwise false
     */
    public boolean sendToDownload(String url, String location, String version){
        try {
            DownloadServiceWorkData payload = new DownloadServiceWorkData(url, location, version);
            LocalDateTime now = LocalDateTime.now();
            long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            WorkTask workTask = new WorkTask("download_task", "information about a component to be downloaded to a specific location", actualTimestamp, payload);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            String message = mapper.writeValueAsString(workTask);
            log.debug("sending message to download service: {}", message);
            natsStreamSenderDownloads().sendWorkMessageToStream(message.getBytes(Charset.defaultCharset()));
            return true;
        } catch (IOException | JetStreamApiException e){
            log.error("an error occurred while trying to send to the downloadService: {}", e.toString());
            return false;
        }

    }
}
