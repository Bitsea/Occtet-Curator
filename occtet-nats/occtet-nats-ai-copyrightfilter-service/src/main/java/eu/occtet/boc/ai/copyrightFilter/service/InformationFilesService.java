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

package eu.occtet.boc.ai.copyrightFilter.service;


import eu.occtet.boc.ai.copyrightFilter.dao.InformationFileDao;
import eu.occtet.boc.ai.copyrightFilter.dao.InformationFileRepository;
import eu.occtet.boc.ai.copyrightFilter.factory.InformationFileFactory;
import eu.occtet.boc.ai.copyrightFilter.retriever.RagHelper;
import eu.occtet.boc.entity.InformationFile;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class InformationFilesService {

    @Autowired
    private InformationFileRepository informationFileRepository;

    @Autowired
    private InformationFileFactory informationFileFactory;

    @Autowired
    private InformationFileDao informationFileDao;

    @Autowired
    VectorStore vectorStore;

    private static final Logger log = LogManager.getLogger(InformationFilesService.class);
    private static final int LIMIT= 20;


    /**
     * retrieve data from normal DB via similarity search for context
     * @param context
     * @return
     */
    public List<InformationFile> retrieveDataByContext(String context){

        List<Pair<UUID, Float>> files= informationFileDao.findInformationFileContentSimilarity(context, LIMIT);
        log.debug("retrieved files from db {}", files.size());
        return files.stream().map(
                        uuidFloatPair ->{
                            InformationFile infoF=informationFileRepository.findById(uuidFloatPair.getKey()).get();
                            return infoF;
                        }).toList();

    }

}
