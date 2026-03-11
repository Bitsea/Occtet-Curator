/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.occtet.boc.model.*;
import eu.occtet.boc.model.BaseSystemMessage;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.ProgressSystemMessage;
import eu.occtet.boc.model.StatusDescriptor;
import eu.occtet.boc.service.NatsHelperService;
import eu.occtet.boc.service.NatsStreamSender;
import eu.occtet.bocfrontend.config.ConfigNatsProperties;
import eu.occtet.bocfrontend.config.ConfigOrtProperties;
import io.nats.client.*;
import io.nats.client.api.*;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling NATS messaging.
 */
@Service
public class NatsService extends NatsHelperService {

    private static final Logger log = LogManager.getLogger(NatsService.class);
    private static final String[] MESSAGES_TO_IGNORE = {"hello","status","exit"};

    @Autowired
    private Connection natsConnection;

    private final ConfigNatsProperties natsProperties;

    public NatsService(ConfigNatsProperties natsProperties) {
        this.natsProperties = natsProperties;
    }

    private @NonNull JetStream js;



    private List<IOnStatusDescriptorReceived> statusDescriptorListeners = new ArrayList<>();

    private List<IOnMicroserviceDescriptorReceived> microserviceDescriptorListeners = new ArrayList<>();

    private List<IOnProgressMessageReceived> progressListeners = new ArrayList<>();

    private StreamInfo stream;
    private ObjectStore objectStore;



    /**
     * Initializes the NATS service, subscribes to the "system" subject, and sets up the JetStream.
     * @throws Exception
     */
    @PostConstruct
    public void onInit() throws Exception {
        subscribeToSystemSubject();
        js = natsConnection.jetStream();
        JetStreamManagement jsm = natsConnection.jetStreamManagement();
        StreamConfiguration config = StreamConfiguration.builder()
                .name(natsProperties.stream_name())
                .subjects(natsProperties.stream_subjects_config())
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .build();
        stream = jsm.addStream(config);
        log.info("initialized NATS stream: " + stream);

        //prepare objectStore
        ObjectStoreManagement objectStoreManagement = natsConnection.objectStoreManagement();

        ObjectStoreConfiguration objectStoreConfiguration = ObjectStoreConfiguration.builder()
                .name("file-bucket")
                .description("bucket containing large files for other microservices")
                .storageType(StorageType.File)
                .compression(true)
                .ttl(Duration.ofHours(Long.parseLong(natsProperties.objectStoreTtl())))
                .build();

        ObjectStoreStatus objectStoreStatus;
        try {
             objectStoreStatus = objectStoreManagement.create(objectStoreConfiguration);
        } catch (JetStreamApiException e) {
            log.warn("Error while trying to create objectStore: {}, deleting old os and creating new one",
                    e.toString());
            objectStoreManagement.delete("file-bucket");
            objectStoreStatus = objectStoreManagement.create(objectStoreConfiguration);
        }

        objectStore = natsConnection.objectStore("file-bucket");
        //setting connection for the helper methods in NatsHelperService
        setNatsConnection(natsConnection);
        log.info("initialized objectsStore: {}", objectStore.getBucketName());
    }

    public String getStreamStatusAsString()  {
        StringBuilder stringBuilder = new StringBuilder();
        StreamState streamState = stream.getStreamState();
        streamState.getSubjectMap().entrySet().forEach(e->{
            stringBuilder.append(e.getKey()+": "+e.getValue()+" messages · ");
        });
        return "messages: " +  streamState.getMsgCount()
                + ", consumers: " + streamState.getConsumerCount()
                + ", subjectCount: " + streamState.getSubjectCount()
                + ", subjects: " + stringBuilder.toString();

    }

    /**
     * subscribe to the "system" subject to receive system messages.
     */
    private void subscribeToSystemSubject() {
        Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {
            String message = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received message on 'system': " + message);
            if(Arrays.asList(MESSAGES_TO_IGNORE).contains(message.toLowerCase())) {
                log.trace("Ignoring message on 'system' subject: " + message);
                return;
            }
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                BaseSystemMessage systemMessage = objectMapper.readerFor(BaseSystemMessage.class).readValue(message);
                if(systemMessage instanceof MicroserviceDescriptor) {
                    MicroserviceDescriptor md = (MicroserviceDescriptor) systemMessage;
                    log.info("Discovered microservice: {} (version {})", md.getName(), md.getVersion());
                    microserviceDescriptorListeners.forEach( listener -> listener.onMicroserviceDescriptorReceived(md) );
                } else if(systemMessage instanceof StatusDescriptor) {
                    StatusDescriptor sd = (StatusDescriptor) systemMessage;
                    log.info("Microservice {} has status: {} with progress {}/100",sd.getName(),sd.getStatus(),sd.getProgressPercent());
                    statusDescriptorListeners.forEach(listener -> listener.onStatusDescriptorReceived(sd));
                } else if(systemMessage instanceof ProgressSystemMessage) {
                    ProgressSystemMessage psm = (ProgressSystemMessage) systemMessage;
                    log.info("Task {} progress: {}/100, details: {}", psm.getTaskId(), psm.getProgressPercent(), psm.getDetails());
                    progressListeners.forEach(listener -> listener.onProgressReceived(psm));
                }

            } catch (JsonProcessingException e) {
                log.debug("Exception processing message on 'system' subject: ", e);
                // this was not a MicroserviceDescriptor message, ignore
            }
        });
        dispatcher.subscribe("system");
    }

    /**
     * Sends a work message to the specified NATS stream (i.e. "work.taskname")
     * @param streamName
     * @param message
     * @throws JetStreamApiException
     * @throws IOException
     */
    public void sendWorkMessageToStream(String streamName, byte[] message) throws JetStreamApiException, IOException {
        log.debug("sending with streamName {}", streamName);
        js.publish(streamName,message);
    }

    public long getNumbOfMsgQueued(String streamSubjectName) {
        try {
            JetStreamManagement jsm = natsConnection.jetStreamManagement();

            StreamInfoOptions options = StreamInfoOptions.builder().filterSubjects(streamSubjectName).build();
            StreamInfo liveStreamInfo = jsm.getStreamInfo(natsProperties.stream_name(), options);

            Map<String, Long> subjectMap = liveStreamInfo.getStreamState().getSubjectMap();
            if (subjectMap.containsKey(streamSubjectName)) {
                return subjectMap.get(streamSubjectName);
            }
            return 0L;
        } catch (JetStreamApiException | IOException e) {
            log.error("Failed to fetch queue size for subject: {}", streamSubjectName, e);
            return 0L;
        }
    }

    /**
     * Sends a simple message to the specified subject to the NATS queue. Mostly used for system messages.
     * @param subject
     * @param message
     */
    public void sendSimpleMessage(String subject, String message) {
        natsConnection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * sends a "hello" message to the "system" subject to request descriptors from other microservices.
     * Should be called on startup of the application.
     */
    public void sendHello() {
        sendSimpleMessage("system","hello");
    }

    public void addMicroserviceDescriptorListener(IOnMicroserviceDescriptorReceived listener) {
        microserviceDescriptorListeners.add(listener);
    }

    public void addStatusDescriptorListener(IOnStatusDescriptorReceived listener) {
        statusDescriptorListeners.add(listener);
    }

    public void addProgressListener(IOnProgressMessageReceived listener) {
        progressListeners.add(listener);
    }


    public byte[] getFileFromBucket(String fileId){
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            objectStore.get(fileId, out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getPreviousExportOfSameProject(String objectStoreKeyTimeStampExcluded){
        try {
            log.debug("Getting previous export of same project with key: {}", objectStoreKeyTimeStampExcluded);
            return objectStore.getList().stream()
                    .map(ObjectInfo::getObjectName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.contains(objectStoreKeyTimeStampExcluded))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error while trying to get previous export of same project: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * Sends a download request message to the download microservice via nats.
     *
     * @param projectId        The ID of the project associated with the download request.
     * @param inventoryItemId  The ID of the inventory item associated with the software component containing the
     *                         download URL.
     * @param isMainPackage    Indicates whether the item to be downloaded is the main package or not.
     * @throws IOException              If there are issues with input/output operations.
     * @throws JetStreamApiException    If there are errors interacting with the messaging stream.
     */
    public void sendToDownload(Long projectId, Long inventoryItemId, Boolean isMainPackage) throws IOException,
            JetStreamApiException{
            DownloadServiceWorkData payload = new DownloadServiceWorkData(projectId, inventoryItemId,
                    isMainPackage);
            LocalDateTime now = LocalDateTime.now();
            long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            WorkTask workTask = new WorkTask(UUID.randomUUID().toString(),"download-service", "information about a component to be downloaded to a specific location", actualTimestamp, payload);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            String message = mapper.writeValueAsString(workTask);
            log.debug("sending message to download service: {}", message);
            sendWorkMessageToStream(natsProperties.send_subject_download(), message.getBytes());
    }
}
