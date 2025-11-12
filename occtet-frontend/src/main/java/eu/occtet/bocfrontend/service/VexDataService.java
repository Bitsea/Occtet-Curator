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


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.occtet.bocfrontend.dao.VexDataRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.factory.VexDataFactory;
import io.jmix.core.FileRef;
import io.jmix.flowui.upload.TemporaryStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VexDataService {

    @Autowired
    private VexDataRepository vexDataRepository;

    @Autowired
    private VexDataFactory vexDataFactory;

    @Autowired
    private TemporaryStorage temporaryStorage;


    private static final Logger log = LogManager.getLogger(VexDataService.class);


    public VexData findBySoftwareComponent(SoftwareComponent softwareComponent) {
        List<VexData> vexData = vexDataRepository.findBySoftwareComponent(softwareComponent);
        if (vexData != null && !vexData.isEmpty()) {
            return vexData.getFirst();
        } else
            return vexDataFactory.create(softwareComponent);
    }

    public FileRef createJsonFile(VexData vexData){
        try {
            String vexIdentifier= vexData.getTimeStamp().toLocalTime().toString().replace(":","-");
            vexIdentifier= vexData.getTimeStamp().toLocalDate()+"-"+vexIdentifier;
            log.debug("Creating VEX JSON file with identifier {}", vexIdentifier);
            String BASEPATH_JSON = "vexData"+vexIdentifier+".json";
            UUID fileID= temporaryStorage.saveFile(BASEPATH_JSON.getBytes());
            ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            mapper.registerModule(new JavaTimeModule());
            mapper.writeValue(temporaryStorage.getFile(fileID), vexData);

            return temporaryStorage.putFileIntoStorage(fileID, BASEPATH_JSON);
            } catch (Exception e) {
                log.error("Error creating VEX JSON file: " + e.getMessage());
                throw new RuntimeException(e);
            }

    }

}
