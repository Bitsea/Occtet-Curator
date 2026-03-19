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

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.ObjectStore;
import io.nats.client.api.ObjectInfo;
import io.nats.client.api.ObjectMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

public abstract class NatsHelperService {

    private static final Logger log = LoggerFactory.getLogger(NatsHelperService.class);

    private Connection natsConnection;

    public void setNatsConnection(Connection natsConnection){
        this.natsConnection= natsConnection;
    }

    public ObjectInfo putDataIntoObjectStore(InputStream data, ObjectMeta metaInformation){
        try {
            ObjectStore objectStore = natsConnection.objectStore("file-bucket");
            ObjectInfo oInfo = objectStore.put(metaInformation, data);
            log.info("Successfully put {} into objectStore:{}", metaInformation.getObjectName(), objectStore.getBucketName());
            return oInfo;
        }catch (JetStreamApiException | IOException | NoSuchAlgorithmException e){
            log.error("Error while trying to put {} into objectStore:{}",metaInformation.getObjectName(), e.toString());
            return null;
        }
    }
}
