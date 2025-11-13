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

package eu.occtet.boc.search.service;

import eu.occtet.boc.model.OpenSearchHit;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.CreatePitResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.Pit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service responsible for searching for terms across openSearch indexed files content
 */
@Service
public class FileSearchService {

    private static final Logger log = LoggerFactory.getLogger(FileSearchService.class);

    private static final String PROJECT_SUFFIX = "project-";
    private static final String CONTENT_FIELD = "content";

    private final OpenSearchClient client;

    public FileSearchService(OpenSearchClient client) {
        this.client = client;
    }

    /**
     * This method is for standard search without pagination.
     * Searches for terms in the content of indexed files using the specified query text
     * and project identifier. Returns a list of search results, each representing a match.
     *
     * @param queryText the text query to search in the indexed content
     * @param projectId the identifier of the project to restrict the search to
     * @param size the maximum number of search results to return
     * @return a list of OpenSearchHit objects representing the results of the search
     * @throws IOException if an I/O error occurs during the search operation
     */
    public List<OpenSearchHit> search(String queryText, String projectId, int size) throws IOException {
        SearchRequest req = buildSearchRequest(queryText, projectId, null, size, null, null, false);
        SearchResponse<JsonData> response = client.search(req, JsonData.class);

        return response.hits().hits().stream()
                .map(this::mapHitToRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Searches for terms in the content of indexed files using the specified query text and project identifier,
     * and processes the search results page by page. Pagination is based on the `searchAfter` mechanism.
     *
     * @param queryText the text query to search in the indexed content
     * @param projectId the identifier of the project to restrict the search to
     * @param pageProcessor a consumer to process each page of search results
     * @param pageSize the number of results per page
     * @param pitTime the time duration to keep the PIT (Point in Time) open for search
     * @throws IOException if an I/O error occurs during the search operation
     */
    public void searchAfter(String queryText, String projectId,
                            Consumer<List<OpenSearchHit>> pageProcessor,
                            int pageSize,
                            String pitTime) throws IOException {

        paginate(queryText, projectId, pageProcessor, pageSize, pitTime, false);
    }

    /**
     * Searches for terms in the content of indexed files using the specified query text and project identifier,
     * and processes the search results page by page in reverse order. Pagination is based on the `searchBefore` mechanism.
     *
     * @param queryText the text query to search in the indexed content
     * @param projectId the identifier of the project to restrict the search to
     * @param pageProcessor a consumer to process each page of search results
     * @param pageSize the number of results per page
     * @param pitTime the time duration to keep the PIT (Point in Time) open for search
     * @throws IOException if an I/O error occurs during the search operation
     */
    public void searchBefore(String queryText, String projectId,
                             Consumer<List<OpenSearchHit>> pageProcessor,
                             int pageSize,
                             String pitTime) throws IOException {

        paginate(queryText, projectId, pageProcessor, pageSize, pitTime, true);
    }

    /**
     * Paginates through OpenSearch results based on a provided query and processes each page of results
     * using the specified page processor. Utilizes a Point-In-Time (PIT) for consistent pagination across
     * the dataset.
     *
     * @param queryText     The search query text used to retrieve results from OpenSearch.
     * @param projectId     The identifier for the project context used for search scoping.
     * @param pageProcessor A consumer to process the list of OpenSearch hits for each page of results.
     * @param pageSize      The number of results to be retrieved and processed per page.
     * @param pitTime       The validity period of the Point-In-Time (PIT) session.
     * @param reverse       A flag indicating whether the results should be returned in reverse order.
     * @throws IOException  If an I/O error occurs during the search or PIT operations.
     */
    private void paginate(String queryText, String projectId,
                          Consumer<List<OpenSearchHit>> pageProcessor,
                          int pageSize,
                          String pitTime,
                          boolean reverse) throws IOException {

        String pitId = createPit(projectId, pitTime);
        List<FieldValue> searchAfterValues = null;

        try {
            while (true) {
                SearchRequest req = buildSearchRequest(queryText, projectId, searchAfterValues, pageSize, pitId, pitTime, reverse);
                SearchResponse<JsonData> response = client.search(req, JsonData.class);

                List<Hit<JsonData>> hits = response.hits().hits();
                if (hits.isEmpty()) break;

                List<OpenSearchHit> results = hits.stream()
                        .map(this::mapHitToRecord)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (reverse) Collections.reverse(results);

                pageProcessor.accept(results);

                searchAfterValues = hits.getLast().sort();
            }
        } finally {
            deletePit(pitId);
        }
    }


    /**
     * Builds a SearchRequest object based on the input parameters. This method is used to construct
     * a search request with query parameters, sorting options, pagination, and point-in-time (PIT) configurations.
     *
     * @param queryText        The text of the query to be searched.
     * @param projectId        The identifier of the project for which the search is performed.
     * @param searchAfterValues A list of field values to use for pagination, indicating the starting point for the next page.
     * @param size             The maximum number of results to return in the search response.
     * @param pitId            The identifier for the point-in-time (PIT) to maintain search context across multiple requests.
     * @param pitTime          The duration for which the PIT should remain active.
     * @param reverse          Determines the sort order. If true, results are sorted in ascending order; otherwise, descending.
     *
     * @return A SearchRequest object configured with the provided parameters.
     */
    private SearchRequest buildSearchRequest(String queryText,
                                             String projectId,
                                             List<FieldValue> searchAfterValues,
                                             int size,
                                             String pitId,
                                             String pitTime,
                                             boolean reverse) {

        Query query = new Query.Builder()
                .multiMatch(m -> m.query(queryText).fields(CONTENT_FIELD))
                .build();

        List<SortOptions> sortOptions = reverse
                ? List.of(
                SortOptions.of(s -> s.field(f -> f.field("_score").order(SortOrder.Asc))),
                SortOptions.of(s -> s.field(f -> f.field("_doc").order(SortOrder.Desc)))
        )
                : List.of(
                SortOptions.of(s -> s.field(f -> f.field("_score").order(SortOrder.Desc))),
                SortOptions.of(s -> s.field(f -> f.field("_doc").order(SortOrder.Asc)))
        );

        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(query)
                .size(size)
                .sort(sortOptions);

        Pit.Builder pitBuilder = new Pit.Builder();
        if (pitId != null) {
            builder.pit(pitBuilder.id(pitId).keepAlive(pitTime).build());
        } else {
            builder.index(PROJECT_SUFFIX + projectId);
        }

        if (searchAfterValues != null) {
            builder.searchAfter(searchAfterValues);
        }

        return builder.build();
    }


    private String createPit(String projectId, String pitTime) throws IOException {
        CreatePitResponse pitResponse = client.createPit(r -> r
                .index(PROJECT_SUFFIX + projectId)
                .keepAlive(t -> t.time(pitTime))
        );
        return pitResponse.pitId();
    }


    private void deletePit(String pitId) {
        try {
            client.deletePit(r -> r.pitId(pitId));
        } catch (Exception e) {
            log.warn("Failed to delete PIT {}", pitId, e);
        }
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
