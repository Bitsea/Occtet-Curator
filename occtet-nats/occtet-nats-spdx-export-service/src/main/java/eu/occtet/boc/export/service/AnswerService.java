/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.export.service;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.ObjectStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service
public class AnswerService {

    private static final Logger log = LogManager.getLogger(AnswerService.class);

    @Autowired
    private Connection natsConnection;

    public void putIntoBucket(String fileName, byte[] data) {
        try {

            ObjectStore objectStore = natsConnection.objectStore("file-bucket");

            try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
                objectStore.put(fileName, input);
                log.info("Successfully uploaded '{}' to object store bucket '{}'", fileName, "file-bucket");
            }

        } catch (IOException | JetStreamApiException | NoSuchAlgorithmException e) {
            log.error("Failed to put '{}' into object store: {}", fileName, e.getMessage(), e);
        }
    }

}