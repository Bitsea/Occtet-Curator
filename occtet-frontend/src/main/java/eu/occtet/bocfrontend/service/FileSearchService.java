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

package eu.occtet.bocfrontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.bocfrontend.model.OpenSearchHit;
import eu.occtet.bocfrontend.model.PaginatedSearchResponse;
import eu.occtet.bocfrontend.model.PaginationDirection;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class responsible for performing search operations on indexed files within a specific project.
 * This class interacts with an OpenSearchClient to execute search requests and retrieve results.
 */
@Service
public class FileSearchService {

    private static final Logger log = LoggerFactory.getLogger(FileSearchService.class);

    private static final String PROJECT_SUFFIX = "project-";
    private static final String CONTENT_FIELD = "content";

    private final ObjectMapper objectMapper = new ObjectMapper();
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
        SearchRequest req = buildSearchRequest(queryText, projectId, null, size, false);
        SearchResponse<JsonData> response = client.search(req, JsonData.class);

        return response.hits().hits().stream()
                .map(this::mapHitToRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Performs a paginated search based on the provided query text, project ID, and pagination settings.
     *
     * @param queryText the search query text to be used for the search
     * @param projectId the ID of the project where the search will be performed
     * @param pageSize the maximum number of results to return per page
     * @param direction the pagination direction, either NEXT or PREVIOUS
     * @param paginationToken the token representing the current pagination position, used for navigating pages
     * @return a PaginatedSearchResponse containing the list of search results, the next pagination token,
     *         and the previous pagination token
     * @throws IOException if an error occurs during the search process
     */
    public PaginatedSearchResponse searchPage(String queryText,
                                              String projectId,
                                              int pageSize,
                                              PaginationDirection direction,
                                              String paginationToken) throws IOException {

        boolean reverse = (direction == PaginationDirection.PREVIOUS);
        List<FieldValue> searchAfterValues = decodeToken(paginationToken);

        SearchRequest req = buildSearchRequest(
                queryText,
                projectId,
                searchAfterValues,
                pageSize,
                reverse
        );
        SearchResponse<JsonData> response = client.search(req, JsonData.class);
        List<Hit<JsonData>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            return new PaginatedSearchResponse(Collections.emptyList(), null, null);
        }

        List<OpenSearchHit> results = hits.stream()
                .map(this::mapHitToRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (reverse) {
            Collections.reverse(results);
        }

        String nextToken = encodeToken(results.getLast().sortValues());
        String prevToken = (direction == PaginationDirection.NONE)
                ? null // 1st page should not have a previous token
                : encodeToken(results.getFirst().sortValues());
        return new PaginatedSearchResponse(results, nextToken, prevToken);
    }

    /**
     * Constructs a SearchRequest object to be used for executing a search operation
     * based on the provided query text, project ID, sorting preferences, and pagination details.
     *
     * @param queryText the search query string to match against the content field
     * @param projectId the identifier of the project to which the search request is scoped
     * @param searchAfterValues a list of values for search-after pagination; can be null
     * @param size the maximum number of results to return in the search response
     * @param reverse a flag indicating whether to reverse the sort order for score and document fields
     * @return a SearchRequest object configured with the specified parameters
     */
    private SearchRequest buildSearchRequest(String queryText,
                                             String projectId,
                                             List<FieldValue> searchAfterValues,
                                             int size,
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
                .index(PROJECT_SUFFIX + projectId)
                .size(size)
                .sort(sortOptions);

        if (searchAfterValues != null) {
            builder.searchAfter(searchAfterValues);
        }

        return builder.build();
    }

    /**
     * Maps a given search hit to an {@link OpenSearchHit} record by converting the hit's source and associated data.
     *
     * @param hit The search hit containing the source and additional metadata such as sorting and score.
     * @return A mapped {@link OpenSearchHit} instance containing the extracted data from the source and hit metadata,
     *         or null if the source is missing.
     */
    private OpenSearchHit mapHitToRecord(Hit<JsonData> hit) {
        if (hit.source() == null) {
            log.warn("Hit without _source field: {}", hit);
            return null;
        }

        OpenSearchHit doc = hit.source().to(OpenSearchHit.class);
        // Extract the hit's sort values (coordinates) to use for the pagination token (req: must be sorted!)
        List<Object> sortData = hit.sort().stream()
                .map(FieldValue::_get)
                .toList();
        return new OpenSearchHit(
                doc.projectId(),
                doc.filePath(),
                doc.lineNumber(),
                doc.content(),
                hit.score() != null ? hit.score() : 0.0,
                sortData
        );
    }

    /**
     * Encodes the provided list of sort values into a URL-safe Base64 encoded string.
     * The method serializes the input list into a JSON byte array and then encodes it.
     * If the input is null or empty, or if an encoding error occurs, the method returns null.
     *
     * @param sortValues the list of sort values to be encoded. It should not be null or empty
     *                   for successful encoding.
     * @return a URL-safe Base64 encoded string representation of the sort values,
     *         or null if the input is invalid or an encoding error occurs.
     */
    private String encodeToken(List<Object> sortValues){
        if (sortValues == null || sortValues.isEmpty()) {
            return null;
        }
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(sortValues);
            return Base64.getUrlEncoder().encodeToString(jsonBytes);
        } catch (JsonProcessingException e) {
            log.error("Failed to encode pagination token with error message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Decodes a base64 URL-encoded token and converts it into a list of FieldValue objects.
     * The token is expected to represent a JSON array with various types of values.
     *
     * @param token the base64 URL-encoded token to be decoded; may be null or empty
     * @return a list of FieldValue objects extracted from the token, or null if the token
     *         is invalid, empty, or contains unsupported data types
     */
    private List<FieldValue> decodeToken(String token){
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(token);

            List<Object> values = objectMapper.readValue(jsonBytes, new TypeReference<>() {});
            List<FieldValue> fieldValues = new ArrayList<>();
            for (Object val : values) {
                switch (val) {
                    case null -> fieldValues.add(FieldValue.of(FieldValue.Builder::nullValue));
                    case String s -> fieldValues.add(FieldValue.of(s));
                    case Integer i -> fieldValues.add(FieldValue.of(i.longValue()));
                    case Long l -> fieldValues.add(FieldValue.of(l));
                    case Double v -> fieldValues.add(FieldValue.of(v));
                    case BigDecimal bigDecimal -> fieldValues.add(FieldValue.of(bigDecimal.doubleValue()));
                    case Boolean b -> fieldValues.add(FieldValue.of(b));
                    default -> {
                        log.warn("Unsupported data type in pagination token: {}", val.getClass());
                        return null;
                    }
                }
            }

            return fieldValues;
        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to deserialize pagination token: {}", token, e);
            return null;
        }
    }
}
