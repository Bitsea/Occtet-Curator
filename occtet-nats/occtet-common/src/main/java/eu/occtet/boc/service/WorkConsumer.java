/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.ProgressSystemMessage;
import eu.occtet.boc.model.WorkTaskProgress;
import eu.occtet.boc.model.WorkTaskStatus;
import eu.occtet.boc.model.WorkerStatus;
import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.FetchConsumeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public abstract class WorkConsumer implements InformativeService {

    private static final Logger log = LoggerFactory.getLogger(WorkConsumer.class);

    protected WorkerStatus workerStatus = WorkerStatus.INIT;
    protected int progressPercent = 0;
    protected String statusDetails = "";

    protected Connection natsConnection;

    private boolean poisonPill = false;

    public void startHandlingMessages(Connection natsConnection, String myServiceName, String streamName,
                                      String workSubject) throws IOException, JetStreamApiException {
        this.natsConnection = natsConnection;
        log.debug("startHandlingMessages called, myServiceName: {}, streamName: {}, workSubject: {}", myServiceName,
                streamName, workSubject);
        JetStream js = natsConnection.jetStream();
        StreamContext streamContext = js.getStreamContext(streamName);
        ConsumerConfiguration config = ConsumerConfiguration.builder()
                .durable(myServiceName + "-consumer")
                .deliverGroup(myServiceName + "-group")
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofMinutes(10))
                .filterSubject(workSubject)
                .build();
        ConsumerContext consumerContext = streamContext.createOrUpdateConsumer(config);
        workerStatus = WorkerStatus.IDLE;

        log.debug("startHandlingMessages called, listening on stream {} for subject {}", streamName, workSubject);

        FetchConsumeOptions fetchOptions = FetchConsumeOptions.builder()
                .maxMessages(1)
                .expiresIn(2000) // 2000ms Long-Polling Timeout
                .build();

        while (natsConnection.getStatus() != Connection.Status.CLOSED) {
            try (FetchConsumer fetchConsumer = consumerContext.fetch(fetchOptions)) {
                Message msg = fetchConsumer.nextMessage();
                if (msg != null && msg.getSubject().equals(workSubject)) {
                    log.debug("received message on subject... {}", msg.getSubject());
                    try {
                        workerStatus = WorkerStatus.WORKING;
                        handleMessage(msg);

                    }catch(Exception e){
                        log.warn("error handling message: {} ({})", e.getMessage(), e.getClass().getSimpleName());

                    }finally{
                        try {
                            msg.ack();
                        } catch (Exception e) {
                            log.error("Failed to ACK message: {}", e.getMessage());
                        }
                        workerStatus = WorkerStatus.IDLE;
                    }
                }
            } catch (Exception e) {
                log.warn("error handling message: {} ({})", e.getMessage(), e.getClass().getSimpleName());
            } finally {
                workerStatus = WorkerStatus.IDLE;
            }

            if (poisonPill)
                return;
        }
    }

    public void terminate() {
        log.debug("shutting down WorkConsumer");
        poisonPill = true;
    }

    /**
     * Implement this method to handle incoming messages
     * 
     * @param msg
     */
    protected abstract void handleMessage(Message msg);

    @Override
    public int getProgressPercent() {
        return progressPercent;
    }

    protected void notifyCompleted(String taskId, String taskName) {
        notifyProgress(taskId, taskName, WorkTaskStatus.COMPLETED, 100, "completed");
    }

    protected void notifyError(String taskId, String taskName, String details) {
        notifyProgress(taskId, taskName, WorkTaskStatus.ERROR, progressPercent, details);
    }

    protected void notifyProgress(String taskId, String taskName, WorkTaskStatus status, int progressPercent,
            String details) {
        this.progressPercent = progressPercent;
        ProgressSystemMessage progressSystemMessage = new ProgressSystemMessage(taskId, taskName, status,
                progressPercent, details);
        String message = null;
        try {
            message = (new ObjectMapper()).writerFor(ProgressSystemMessage.class)
                    .writeValueAsString(progressSystemMessage);
        } catch (JsonProcessingException e) {
            log.warn("error creating progress message: {}", e.getMessage());
        }
        log.debug("notifying progress: taskId {} has now progress {}", taskId, progressPercent);
        natsConnection.publish("system", message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public WorkerStatus getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(WorkerStatus workerStatus) {
        this.workerStatus = workerStatus;
    }

    @Override
    public String getStatusDetails() {
        return statusDetails;
    }

    public void setStatusDetails(String statusDetails) {
        this.statusDetails = statusDetails;
    }

}
