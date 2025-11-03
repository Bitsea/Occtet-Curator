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

package eu.occtet.boc.fileindexing;


import eu.occtet.boc.fileindexing.configuration.OpenSearchClientConfig;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = OpenSearchClientConfig.class)
public class OpenSearchIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchIntegrationTest.class);

    @Autowired
    private OpenSearchClient client;

    public static class TestDoc {
        private String content;
        private String user;

        public TestDoc(String content, String user) {
            this.content = content;
            this.user = user;
        }

        public TestDoc() {}

        public String getContent() { return content; }
        public String getUser() { return user; }
    }

    @Test
    public void testIndexAndTermSearch() throws IOException {
        String indexName = "junit-test-index";
        String docId = "doc-1";
        String testUser = "test-user-exact";

        log.info("Creating index: {}", indexName);
        TestDoc doc = new TestDoc("This is a test file for JUnit.", testUser);

        IndexRequest<TestDoc> indexRequest = new IndexRequest.Builder<TestDoc>()
                .index(indexName)
                .id(docId)
                .document(doc)
                .build();

        client.index(indexRequest);
        log.info("Document indexed successfully");

        client.indices().refresh(r -> r.index(indexName));
        log.info("Indices refreshed. Performing term search for user: {}", testUser);

        SearchResponse<TestDoc> searchResponse = client.search(s -> s
                .index(indexName)
                .query(q -> q
                        .term(t -> t
                                .field("user.keyword")
                                .value(FieldValue.of(testUser))
                        )
                ), TestDoc.class
        );

        long totalHits = searchResponse.hits().total().value();
        log.info("Found {} hits", totalHits);

        assertEquals(1, totalHits);
        assertEquals(docId, searchResponse.hits().hits().get(0).id());
        assertEquals(testUser, searchResponse.hits().hits().get(0).source().getUser());
    }
}