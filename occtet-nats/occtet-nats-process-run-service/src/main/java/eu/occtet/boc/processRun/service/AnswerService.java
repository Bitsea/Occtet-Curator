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

package eu.occtet.boc.processRun.service;

import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.service.NatsStreamSender;
import io.nats.client.Connection;
import io.nats.client.api.ObjectInfo;
import io.nats.client.api.ObjectMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

@Service
public class AnswerService {

    private static final Logger log = LogManager.getLogger(AnswerService.class);

    @Autowired
    private Connection natsConnection;



    @Value("${nats.send-subject}")
    private String sendSubject;

    @Bean
    public NatsStreamSender natsStreamSender(){
        return new NatsStreamSender(natsConnection, sendSubject);
    }

    public boolean sendToSpdxService(File file, Long projectId, boolean useCopyright, boolean useLicenseMatch){

        try{
        FileInputStream inputStream = new FileInputStream(file);
        ByteArrayInputStream objectStoreInput = new ByteArrayInputStream(spdxJson);

        ObjectMeta objectMeta = ObjectMeta.builder(file.getAbsolutePath())
                .description("Spdxdocument for use by spdx-microservice")
                .chunkSize(32 * 1024)
                .build();

        ObjectInfo objectInfo = natsConnection.objectStoreManagement().natsService.putDataIntoObjectStore(objectStoreInput, objectMeta);
        if (objectInfo == null) return false;

        SpdxWorkData spdxWorkData = new SpdxWorkData(objectInfo.getObjectName(), objectInfo.getBucket(), projectId, useCopyright, useLicenseMatch);
        return true;

        } catch(Exception e){
            log.error("Error sending SPDX data to microservice", e);
            return false;
        }

    }
}
