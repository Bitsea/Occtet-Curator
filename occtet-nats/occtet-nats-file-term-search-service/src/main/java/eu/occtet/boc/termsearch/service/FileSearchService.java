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

package eu.occtet.boc.termsearch.service;

import eu.occtet.boc.model.OpenSearchHit;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for searching for terms across openSearch indexed files content
 */
@Service
public class FileSearchService {

    private static final Logger log = LoggerFactory.getLogger(FileSearchService.class);
    private static final String PROJECT_SUFFIX = "project-";

    private final OpenSearchClient client;

    public FileSearchService(OpenSearchClient client) {
        this.client = client;
    }

    /**
     * This method searches a project for the given text query and returns a list containing the findings
     * @param queryText text to search for
     * @param projectId the target project id
     * @param size the number of results in the list
     * @return a list of OpenSearchHit containing data such as filePath and lineNumber
     * @throws IOException IOException If the search request to OpenSearch fails.
     */
    public List<OpenSearchHit> search(String queryText, String projectId, int size) throws IOException {
        SearchRequest req = new SearchRequest.Builder()
                .index(PROJECT_SUFFIX + projectId)
                .query(q -> q
                        .multiMatch(m -> m
                                .query(queryText)
                                .fields("content")
                        )
                )
                .size(size)
                .build();

        log.info("Running search for: {}", queryText);
        log.debug("Search request JSON: {}", req.toJsonString());

        SearchResponse<JsonData> response = client.search(req, JsonData.class);
        log.debug("Search response JSON: {}", response.toJsonString());

        TotalHits total = response.hits().total();
        log.info("Found {} hits. Relation: {}",
                total != null ? total.value() : 0,
                total != null ? total.relation() : "unknown"
        );

        return response.hits().hits().stream().map(this::mapHitToRecord).collect(Collectors.toList());
    }

    private OpenSearchHit mapHitToRecord(Hit<JsonData> hit) {
        if (hit.source() == null) {
            log.warn("Hit without _source field: {}", hit);
            return null;
        }

        OpenSearchHit doc = hit.source().to(OpenSearchHit.class);

        return new OpenSearchHit(
                doc.projectId(),
                doc.filePath(),
                doc.lineNumber(),
                doc.content(),
                hit.score() != null ? hit.score() : 0.0
        );
    }
}
