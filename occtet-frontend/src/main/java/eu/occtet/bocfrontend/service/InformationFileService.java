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
package eu.occtet.bocfrontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.occtet.boc.model.InformationFileSendWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.scanner.SpdxScanner;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;



@Service
public class InformationFileService {

    private static final Logger log = LogManager.getLogger(SpdxScanner.class);

    private final static Path tmpLocation = Path.of("src","main","resources/TmpInfoFilePackage");

    @Autowired
    private NatsService natsService;

    public boolean uploadInformationFile(String path, String context){

        InformationFileSendWorkData workData = new InformationFileSendWorkData(path,context);
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("informationFile_task", "upload information file", actualTimestamp, workData);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            String message = mapper.writeValueAsString(workTask);
            log.debug("sending message to informationFile microservice: {}", message);
            natsService.sendWorkMessageToStream("work.InformationFile", message.getBytes(Charset.defaultCharset()));

            return true;
        }catch(Exception e){
            log.error("Error with microservice connection: {}", e.getMessage());
            return false;
        }
    }

    public File createTempInformationFile(FileUploadSucceededEvent<FileUploadField> event){

        try{
            if(Files.notExists(tmpLocation)){
                Files.createDirectories(Path.of(tmpLocation.toFile().getAbsolutePath()));
            }
            String fileName = event.getFileName();
            byte[] fileContent = event.getSource().getValue();
            File file = new File(tmpLocation.toFile().getAbsolutePath(), fileName);
            FileOutputStream fos = new FileOutputStream(file);

            if(fileContent != null) {
                fos.write(fileContent);
            }
            fos.close();
            return file;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
