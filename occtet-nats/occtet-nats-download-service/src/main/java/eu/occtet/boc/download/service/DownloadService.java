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


import eu.occtet.boc.model.DownloadServiceWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class DownloadService extends BaseWorkDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    @Autowired
    private GitService gitService;

    @Autowired
    private ZipService zipService;

    @Override
    public boolean process(DownloadServiceWorkData workData) {
        log.debug("DownloadService: downloading data from {} to {}", workData.getUrl(),workData.getLocation());
        return storeData(workData);
    }

    public boolean storeData(DownloadServiceWorkData workData){

        try{
            String analyzedUrl = analyzingUrl(workData.getUrl());

            if(analyzedUrl.equals("zip")|| analyzedUrl.equals("jar")){
                zipService.editJarFile(workData.getUrl(),workData.getLocation());

            }else if(analyzedUrl.equals("git")){
                gitService.unpackRepo(workData.getUrl(),workData.getLocation());
            }
        }catch (Exception e){
            log.error("Error in storeData...");
            return false;
        }
        return true;
    }

    private String analyzingUrl(String url){
        int index = url.lastIndexOf('.');
        return url.substring(index,url.length()-1);
    }

}
