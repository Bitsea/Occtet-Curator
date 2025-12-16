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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.*;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.factory.CopyrightFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jmix.core.FileRef;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.upload.TemporaryStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CopyrightService {

    @Autowired
    private CopyrightFactory copyrightFactory;

    private static final Logger log = LogManager.getLogger(CopyrightService.class);



    private static final Path BASEPATH_YML = Paths.get("src", "main", "resources","garbage-Copyrights","garbage-copyrights.yml");
    private static final Path BASEPATH_JSON = Paths.get("src","main","resources","garbage-Copyrights","garbage-copyrights.json");
    private static final String FILENAME_YML = "garbage-copyrights.yml";

    @Autowired
    private TemporaryStorage temporaryStorage;




    public void createYML(List<Copyright> copyrightList) {

        try {
            Map<String, List<String>> data = new HashMap<>();
            data.put("Copyrights", copyrightList.stream().map(Copyright::getCopyrightText).toList());
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writeValue(new File(BASEPATH_YML.toFile().getAbsolutePath()), data);
        } catch (IOException e) {
            log.debug("Error creating YML file: " + e.getMessage());
        }
    }

    public FileRef getYmlFileRef() {

        try {
            FileInputStream file = new FileInputStream(BASEPATH_YML.toFile().getAbsolutePath());
            UUID id = temporaryStorage.saveFile(file.readAllBytes());
            FileRef ref = temporaryStorage.putFileIntoStorage(id, FILENAME_YML);
            return ref;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> readYML(File file) {

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, List<String>> dataYML = mapper.readValue(file, Map.class);
            String key = dataYML.keySet().stream().toList().get(0);
            return dataYML.get(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setGarbageCopyrightsInJSON(List<String> garbageCopyrights) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            File jsonFile = new File(BASEPATH_JSON.toFile().getAbsolutePath());
            Map<String,List<String>> garbage;

            if (jsonFile.exists() && jsonFile.length() > 0) {
                garbage = mapper.readValue(jsonFile,  new TypeReference<>() {});
            } else {
                garbage  = new HashMap<>();
            }
            List<String> garbageListe = garbage.computeIfAbsent("Copyright", k -> new ArrayList<>());

            if (!(garbageListe instanceof ArrayList)) {
                garbageListe = new ArrayList<>();
                garbage.put("Copyright", garbageListe);
            }
            for(String copyright : garbageCopyrights){
                if(!garbageListe.contains(copyright)){
                    garbageListe.add(copyright);
                }
            }
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(jsonFile, garbage);

        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public File createFileUploadCopyrights(FileUploadSucceededEvent<FileUploadField> event){

        try{
            String fileName = event.getFileName();
            byte[] fileContent = event.getSource().getValue();
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);

            if(fileContent != null) {
                fos.write(fileContent);
            }
            return file;

        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public Copyright createCopyright(String name, List<CodeLocation> codeLocation, boolean isCurated, boolean isGarbage){
        return copyrightFactory.create(name,codeLocation,isCurated,isGarbage);
    }
}


