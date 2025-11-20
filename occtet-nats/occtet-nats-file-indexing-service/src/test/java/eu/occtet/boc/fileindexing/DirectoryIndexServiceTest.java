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

package eu.occtet.boc.fileindexing;import eu.occtet.boc.fileindexing.service.DirectoryIndexService;
import eu.occtet.boc.fileindexing.service.FileIndexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = DirectoryIndexService.class)
@ExtendWith(MockitoExtension.class)
public class DirectoryIndexServiceTest {

    @MockitoBean
    private FileIndexService fileIndexService;
    @Autowired
    private DirectoryIndexService directoryIndexService;

    @Test
    void shouldWalkDirectoryAndTriggerIndexing(@TempDir Path tempDir) throws IOException {
        Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Path file2 = Files.createFile(subDir.resolve("file2.java"));
        Files.createDirectory(tempDir.resolve("emptyDir"));

        String projectId = "test-project";

        when(fileIndexService.indexFileByLines(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        directoryIndexService.indexDirectory(tempDir.toString(), projectId);
        verify(fileIndexService, times(1)).indexFileByLines(eq(file1.toString()), eq(projectId));
        verify(fileIndexService, times(1)).indexFileByLines(eq(file2.toString()), eq(projectId));
        verify(fileIndexService, times(2)).indexFileByLines(anyString(), anyString());
    }
}
