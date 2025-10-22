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

package eu.occtet.boc.download.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.jar.JarFile;

@Service
public class ZipService {

    private static final Logger log = LoggerFactory.getLogger(ZipService.class);

    public void editJarFile(String url, String location){

        try{
            JarFile jarFile = new JarFile(url);
            jarFile.stream().forEach(jarEntry -> {

                File file = new File(location,jarEntry.getRealName());
                if(!file.isDirectory()){
                    try {
                        InputStream in = jarFile.getInputStream(jarEntry);
                        FileOutputStream outputFile = new FileOutputStream(file);
                        in.transferTo(outputFile);

                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
