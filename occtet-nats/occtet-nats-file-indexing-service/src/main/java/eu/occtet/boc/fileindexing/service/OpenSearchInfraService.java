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
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.ExistsIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.PutIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.put_index_template.IndexTemplateMapping;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Service responsible for setting up OpenSearch infrastructure, such as index templates.
 * This should be run on application startup
 */
@Service
public class OpenSearchInfraService {
    private static final Logger log = LoggerFactory.getLogger(OpenSearchInfraService.class);
    private static final String PROJECT_INDEX_TEMPLATE_NAME = "project-files-template";
    private static final String PROJECT_INDEX_PATTERN = "project-*";

    private final OpenSearchClient client;

    public OpenSearchInfraService(OpenSearchClient client) {
        this.client = client;
    }

    /**
     * This method is to check whether the indexing template exists, and if not, it creates it.
     * It also defines the mappings for all project indices.
     */
    public void ensureIndexTemplate() throws IOException{

        // check existence
        ExistsIndexTemplateRequest existingReq = new ExistsIndexTemplateRequest.Builder()
                .name(PROJECT_INDEX_TEMPLATE_NAME)
                .build();
        BooleanResponse existsRe = client.indices().existsIndexTemplate(existingReq);
        if (existsRe.value()) {
            log.info("Index template '{}' already exists. No action needed.", PROJECT_INDEX_TEMPLATE_NAME);
            return;
        }
        log.info("Index template '{}' not found. Creating...", PROJECT_INDEX_TEMPLATE_NAME);

        // define mappings
        Map<String, Property> properties = Map.of(
                "projectId", new Property.Builder().keyword(k -> k).build(),
                "filePath", new Property.Builder().keyword(k -> k).build(),
                "lineNumber", new Property.Builder().integer(i -> i).build(),
                "content", new Property.Builder().text(t -> t).build() // split in tokens
        );
        TypeMapping typeMapping = new TypeMapping.Builder()
                .properties(properties)
                .build();

        // configure settings
        IndexSettings settings = new IndexSettings.Builder()
                .numberOfShards(1)
                .numberOfReplicas(1)
                .build();

        IndexTemplateMapping template =
                new IndexTemplateMapping.Builder().settings(settings).mappings(typeMapping).build();

        // template request creation
        PutIndexTemplateRequest request = new PutIndexTemplateRequest.Builder()
                .name(PROJECT_INDEX_TEMPLATE_NAME)
                .indexPatterns(Collections.singletonList(PROJECT_INDEX_PATTERN))
                .template(template)
                .build();

        PutIndexTemplateResponse response = client.indices().putIndexTemplate(request);

        if (response.acknowledged()){
            log.info("Successfully created index template '{}' with response message: {}.", PROJECT_INDEX_TEMPLATE_NAME,
                    response.toJsonString());
        } else {
            log.warn("Failed to create index template '{}' with response message: {}.", PROJECT_INDEX_TEMPLATE_NAME,
                    response.toJsonString());
            throw new IOException("Failed to create index template: " + PROJECT_INDEX_TEMPLATE_NAME);
        }

    }

}
