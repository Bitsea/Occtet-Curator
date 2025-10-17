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

package eu.occtet.boc.service;

import eu.occtet.boc.model.WorkerStatus;
import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public abstract class WorkConsumer implements InformativeService {

    private static final Logger log = LoggerFactory.getLogger(WorkConsumer.class);

    protected WorkerStatus workerStatus = WorkerStatus.INIT;
    protected int progressPercent=0;
    protected String statusDetails="";

    protected Connection natsConnection;

    private boolean poisonPill=false;

    public void startHandlingMessages(Connection natsConnection, String myServiceName, String streamName, String workSubject) throws IOException, JetStreamApiException {
        this.natsConnection = natsConnection;
        JetStream js = natsConnection.jetStream();
        StreamContext streamContext = js.getStreamContext(streamName);
        ConsumerConfiguration config = ConsumerConfiguration.builder()
                .durable(myServiceName+"-consumer")
                .deliverGroup(myServiceName+"-group")
                .ackPolicy(AckPolicy.Explicit)
                .filterSubject(workSubject)
                .build();
        ConsumerContext consumerContext = streamContext.createOrUpdateConsumer(config);
        workerStatus= WorkerStatus.IDLE;

        log.debug("startHandlingMessages called, listening on stream {} for subject {}", streamName, workSubject);

        while(true) {
            try (FetchConsumer fetchConsumer = consumerContext.fetchMessages(1)) {
                Message msg= fetchConsumer.nextMessage();
                if (msg != null) {
                    log.debug("received message on subject... {}", msg.getSubject());
                    msg.ack();

                    workerStatus=WorkerStatus.WORKING;

                    handleMessage(msg);
                }
            } catch (Exception e) {
                log.warn("error handling message: {}", e.getMessage());
            }
            finally {
                workerStatus=WorkerStatus.IDLE;
            }
            // if someone wants us to stop, we stop
            if(poisonPill) return;
        }
    }

    public void terminate() {
        log.debug("shutting down WorkConsumer");
        poisonPill=true;
    }

    /**
     * Implement this method to handle incoming messages
     * @param msg
     */
    protected abstract void handleMessage(Message msg);

    @Override
    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
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
