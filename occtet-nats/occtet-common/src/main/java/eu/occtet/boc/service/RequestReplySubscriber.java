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

package eu.occtet.boc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public abstract class RequestReplySubscriber<REQ, RES> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected Connection natsConnection;
    protected ObjectMapper objectMapper;

    protected abstract String getSubject();
    protected abstract Class<REQ> getRequestClass();

    /**
     * The concrete business logic to execute for a request.
     *
     * @param request The deserialized request object.
     * @return The response object to be sent back.
     * @throws Exception If an error occurs during processing.
     */
    protected abstract RES handleRequest(REQ request) throws Exception;
    protected abstract RES handleError(Exception e);

    /**
     * Initializes and starts the subscription.
     * This is the explicit entry point, analogous to WorkConsumer's startHandlingMessages.
     *
     * @param natsConnection The NATS connection.
     * @param objectMapper The ObjectMapper for serialization.
     */
    public void startSubscription(Connection natsConnection, ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;

        // Create a Core NATS Dispatcher (not JetStream)
        Dispatcher dispatcher = natsConnection.createDispatcher(this::onMessageReceived);

        dispatcher.subscribe(getSubject());

        log.info("Started NATS Request-Reply subscriber for subject: {}", getSubject());
    }


    /**
     * Internal message handler that wraps the abstract business logic.
     * This is the "engine" of the Request-Reply pattern.
     *
     * @param msg The incoming NATS message.
     */
    private void onMessageReceived(Message msg) {
        String replyTo = msg.getReplyTo();
        if (replyTo == null || replyTo.isEmpty()) {
            log.warn("Received message on {} without a 'replyTo' subject. Ignoring.", getSubject());
            return;
        }
        RES response;
        try {
            String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
            REQ req = objectMapper.readValue(jsonData, getRequestClass());

            response = handleRequest(req);
        } catch (Exception e) {
            log.error("Error handling request", e.getMessage());
            response = handleError(e);
        }
        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(response);
            natsConnection.publish(replyTo, responseBytes);
        } catch (JsonProcessingException e) {
            log.error("Error serializing response", e.getMessage());
        }
    }
}
