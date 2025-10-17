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

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending NATS messaging.
 */

public class NatsStreamSender {

    private static final Logger log = LoggerFactory.getLogger(NatsStreamSender.class);


    private Connection natsConnection;


    private String subjectName;


    public NatsStreamSender(Connection natsConnection, String subjectName) {
        this.natsConnection = natsConnection;
        this.subjectName = subjectName;
    }

    /**
     * Sends a work message to the specified NATS stream (i.e. "work.taskname")
     * @param message
     * @throws JetStreamApiException
     * @throws IOException
     */
    public void sendWorkMessageToStream(byte[] message) throws JetStreamApiException, IOException {
        natsConnection.jetStream().publish(subjectName,message);
    }


}
