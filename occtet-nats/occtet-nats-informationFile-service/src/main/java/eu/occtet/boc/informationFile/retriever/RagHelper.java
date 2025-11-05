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


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;


public final class RagHelper {

    private static final Logger log = LogManager.getLogger(RagHelper.class);


    /**
     * default
     * splits the documents before putting them into the vector store
     * @param documents
     * @return
     */
    public static List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    /**
     * customized chunksize etc.
     * splits the documents before putting them into the vector store
     * @param documents
     * @return
     */
    public static List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(350, 100, 10, 500, true);
        for(Document d: documents) {
            log.debug("2 Files added to list {} metadata {}", documents.size(), d.getMetadata());
        }
        return splitter.apply(documents);
    }
}