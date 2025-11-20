/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.fileindexing.service;


import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for indexing files into OpenSearch.
 * Uses the Bulk API for efficient indexing.
 */
@Service
public class FileIndexService {

    private static final Logger log = LoggerFactory.getLogger(FileIndexService.class);
    private static final String INDEX_SUFFIX = "project-"; // should match the pattern defined in the template
    private static final int BULK_BATCH_SIZE = 1000;

    private final OpenSearchClient client;

    /**
     * Represents the doc structure for each indexed line.
     */
    private record FileLineDocument(String projectId, String filePath, int lineNumber, String content) {}


    public FileIndexService(OpenSearchClient client) {
        this.client = client;
    }


    /**
     * Indexes a file by reading it line-by-line and sending lines in batches using the OpenSearch Bulk API.
     *
     * @param filePath the path of the file to be indexed
     * @param projectId the ID of the project used to determine the target OpenSearch index
     * @return a CompletableFuture that completes when the operation is finished or fails if an error occurs
     */
    @Async("fileIndexingExecutor")
    public CompletableFuture<Void>  indexFileByLines(String filePath, String projectId) {
        try{
            String indexName = INDEX_SUFFIX + projectId;

            List<FileLineDocument> batch = new ArrayList<>(BULK_BATCH_SIZE);
            int lineNumber = 0;

            try (InputStream inputStream = new FileInputStream(filePath);
                 BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    batch.add(new FileLineDocument(projectId, filePath, lineNumber, line));

                    if (batch.size() >= BULK_BATCH_SIZE){
                        log.info("Sending batch of {} lines for file: {}", batch.size(), filePath);
                        sendBulkRequest(indexName, batch);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()){
                log.debug("Sending final batch of {} lines for file: {}", batch.size(), filePath);
                sendBulkRequest(indexName, batch);
            }
            log.info("Finished indexing file: {}. Total lines processed: {}", filePath, lineNumber);

            return CompletableFuture.completedFuture(null);
        } catch (IOException e){
            log.error("Failed to index file: {}. Error: {}", filePath, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Constructs and sends a BulkRequest to OpenSearch.
     *
     * @param indexName the name of the OpenSearch index where the documents will be stored
     * @param documents a list of documents to be indexed
     * @throws IOException if an error occurs during the sending of the bulk request
     */
    private void sendBulkRequest(String indexName, List<FileLineDocument> documents) throws IOException {
        if (documents.isEmpty()){
            return;
        }
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (FileLineDocument doc : documents){
            String docId = generateDocumentId(doc);
            bulkBuilder.operations(op -> op.index(idx -> idx.index(indexName).id(docId).document(doc)));
        }
        log.debug("Sending {} documents to index: {}", documents.size(), indexName);
        BulkResponse res = client.bulk(bulkBuilder.build());
        log.debug("Bulk request response: {}", res.toJsonString());
        if (res.errors()){
            log.error("Bulk request failed with errors: {}", res.toJsonString());
            for (BulkResponseItem item : res.items()){
                if (item.error() != null){
                    log.error("Failed to index document {}: {}", item.id(), item.error().reason());
                }
            }
        }
    }

    private String generateDocumentId(FileLineDocument doc){
        String id = doc.filePath() + ":" + doc.lineNumber();
        return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
