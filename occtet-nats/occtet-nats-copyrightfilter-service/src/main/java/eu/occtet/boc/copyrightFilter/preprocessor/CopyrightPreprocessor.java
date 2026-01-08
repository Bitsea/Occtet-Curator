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

package eu.occtet.boc.copyrightFilter.preprocessor;


import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.CopyrightModel;
import eu.occtet.boc.model.CopyrightText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class CopyrightPreprocessor {


    private static final Logger log = LogManager.getLogger(CopyrightPreprocessor.class);


    /**
     * Reads the garbage copyrights from a json file into a list of strings
     * @return list of garbage copyrights
     */
    public List<String> readGarbageCopyrightsFromJson(Path path) {
        log.debug("readGarbageCopyrightsFromJson called with path: {}", path.toFile().getAbsolutePath());
        CopyrightText garbageCopyright= new CopyrightText();
        List<String> copyrightTexts= new ArrayList<>();
        File jsonFile = new File(path.toFile().getAbsolutePath());
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream is= new FileInputStream(jsonFile);
            garbageCopyright = objectMapper.readValue(is, CopyrightText.class);
            copyrightTexts = garbageCopyright.getCopyright();
            for(String ct: copyrightTexts){
                log.debug("read garbage copyright: {}", ct);
            }
        }catch(Exception e){
            log.error("error reading json file: {}", e.getMessage());
        }
        return copyrightTexts;
    }

    /**
     * Trims away everything that is not copyright information from an ORT scan-result file
     * @param fileName name of the .yml file with the scan-results
     * @return a list of copyright records
     * @throws IOException
     */

    public List<CopyrightModel> trimCopyrightYml(String fileName){
       try {
           List<CopyrightModel> copyrightModels = new ArrayList<>();
           boolean isInTargetSection = false;
           File dir = new File(fileName);
            if(dir.exists() & dir.canRead()) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(dir)));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("copyrights:")) {
                        isInTargetSection = true;
                        // Skip the "copyrights:" line
                        log.info("found copyright section within .yml file");
                        continue;
                    }
                    //after the copyright section when reaching the "- provenance:" line
                    if (line.trim().startsWith("- provenance:")) {
                        isInTargetSection = false;
                        // Skip the "- provenance:" line
                        continue;
                    }
                    //look for the start of a copyright statement within the copyrights: section
                    if (isInTargetSection && line.trim().startsWith("- statement")) {
                        //remove leading "- statement: " and leftover " characters
                        String statement = line.trim().substring(13).replace("\"", "");
                        reader.readLine(); //skip empty "location:" line
                        //remove leading "path: " and leftover " characters
                        String path = reader.readLine().trim().substring(6).replace("\"", "");
                        //remove leading "start_line: "
                        int start_line = Integer.parseInt(reader.readLine().trim().substring(12));
                        //remove leading "end_line: "
                        int end_line = Integer.parseInt(reader.readLine().trim().substring(10));
                        copyrightModels.add(new CopyrightModel(statement, path, start_line, end_line));
                    }
                }
                return copyrightModels;
            }else{
                log.error("File not existing? {}", fileName);
                return null;
            }

       }catch(Exception e){
           log.error("copyright file could not be processed {}", e.getMessage());
           return null;
       }
    }




}