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

package eu.occtet.boc.informationFile.retriever;

import eu.occtet.boc.entity.InformationFile;
import eu.occtet.boc.informationFile.service.DBLogService;
import eu.occtet.boc.informationFile.service.InformationFileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CopyrightRetriever {

    private static final Logger log = LogManager.getLogger(CopyrightRetriever.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private InformationFileService informationFileService;

    @Autowired
    private DBLogService dbLogService;

    private static final int ZERO = 0;

    /**
     * loading copyright files into the vector store with a context helping for embedding and retrieval
     * has to be changed, so the path to documents is not hardcoded
     * @param context
     * @throws IOException
     */
    public void loadVectorStore(String context)  {
        List<Document> documentList = new ArrayList<>();

        List<InformationFile> files = informationFileService.retrieveDataByContext(context);
        //if there are no files no upload needed
        if(!files.isEmpty()) {
            log.debug("Files from DB {} first filename {}", files.size(), files.get(ZERO).getFileName());

            List<Document> documents = new ArrayList<>();
            try {
                for (InformationFile f : files) {
                    Document d = new Document(f.getContent(), Map.of("fileName", f.getFileName()));
                    d.getMetadata().put("filePath", f.getFileName());
                    d.getMetadata().put("context", f.getContext());
                    documents.add(d);
                    //TODO better log message for DB
                    dbLogService.saveLog("Retriever", "Found a document: " + f.getFileName() + "\n With content: \n: " + f.getContent());
                }

            } catch (Exception e) {
                log.error("Error while reading the files : {}", e.getMessage());
            }
            log.debug("Files added to list {} metadata {}", documents.size(), documents.get(ZERO).getMetadata());

            documentList = RagHelper.splitCustomized(documents);

            log.debug("documentlist {} metadata {}", documentList.size(), documentList.get(ZERO).getMetadata());

            vectorStore.add(documentList);
            //TODO better log message for DB
            dbLogService.saveLog("Retriever", "Loaded " + documentList.size() + " Chunks into VectorDB");
        }
        else{
            log.info("No files found to load into Vector-DB");
        }
    }

}
