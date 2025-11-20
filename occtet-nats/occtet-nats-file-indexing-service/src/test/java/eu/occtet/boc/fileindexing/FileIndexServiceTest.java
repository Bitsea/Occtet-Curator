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

import eu.occtet.boc.fileindexing.service.FileIndexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = FileIndexService.class)
@ExtendWith(MockitoExtension.class)
public class FileIndexServiceTest {

    @MockitoBean
    private OpenSearchClient openSearchClient;
    @Autowired
    private FileIndexService fileIndexService;

    @Test
    void shouldIndexFileWhenFileExists(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException {
        Path testFile = tempDir.resolve("testFile.txt");
        List<String> lines = List.of("Line 1 content", "Line 2 content", "Line 3 content");
        Files.write(testFile, lines);

        String projectId = "test-project";

        BulkResponse mockResponse = mock(BulkResponse.class);
        when(mockResponse.errors()).thenReturn(false);
        when(mockResponse.toJsonString()).thenReturn("{}");
        when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(mockResponse);

        CompletableFuture<Void> future = fileIndexService.indexFileByLines(testFile.toString(), projectId);

        future.get();

        verify(openSearchClient, times(1)).bulk(any(BulkRequest.class));

        assertTrue(future.isDone());
    }
}
