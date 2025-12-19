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

package eu.occtet.boc.download;

import com.sun.net.httpserver.HttpServer;
import eu.occtet.boc.download.controller.GitRepoController;
import eu.occtet.boc.download.dao.InventoryItemRepository;
import eu.occtet.boc.download.dao.ProjectRepository;
import eu.occtet.boc.download.service.DownloadService;
import eu.occtet.boc.download.service.FileService;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.model.DownloadServiceWorkData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@EntityScan(basePackages = "eu.occtet.boc.entity")
public class DownloadServiceTest {
    @Mock
    private GitRepoController gitRepoController;
    @Mock
    private FileService fileService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @InjectMocks
    private DownloadService downloadService;

    @TempDir
    Path tempDir;
    private HttpServer localServer;
    private int localPort;
    private byte[] dummyZipBytes;

    @BeforeEach
    void setUp() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry = new ZipEntry("dummy-project/dummy-main/dummy-file.txt");
            zos.putNextEntry(entry);
            zos.write("Dummy File Content".getBytes());
            zos.closeEntry();

            ZipEntry readme = new ZipEntry("README.md");
            zos.putNextEntry(readme);
            zos.write("Read me".getBytes());
            zos.closeEntry();

            dummyZipBytes = baos.toByteArray();
        }

        localServer = HttpServer.create(new InetSocketAddress(0), 0);
        localServer.createContext("/dummy-repo.zip", exchange -> {
            exchange.sendResponseHeaders(200, dummyZipBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(dummyZipBytes);
            }
        });
        localServer.setExecutor(null);
        localServer.start();
        localPort = localServer.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (localServer != null) localServer.stop(0);
    }

    @Test
    void testProcessValidPathHttp() {
        String project = UUID.randomUUID().toString();
        String inventoryItemId = UUID.randomUUID().toString();
        String downloadUrl = "http://localhost:" + localPort + "/dummy-repo.zip";
        Path targetLocation = tempDir.resolve("project_temp_root");
        // Main Package
        DownloadServiceWorkData workData = new DownloadServiceWorkData(downloadUrl, targetLocation.toString(),
                "1.0.0", project, true, inventoryItemId);
        when(projectRepository.findById(UUID.fromString(project))).thenReturn(Optional.of(new Project()));
        when(inventoryItemRepository.findById(UUID.fromString(inventoryItemId))).thenReturn(Optional.of(new InventoryItem()));
        boolean result = downloadService.process(workData);
        assertTrue(result);

        Path expectedFile = targetLocation.resolve("dummy-repo").resolve("1.0.0")
                .resolve("dummy-project").resolve("dummy-main").resolve("dummy-file.txt");
        assertTrue(Files.exists(expectedFile), "Main package file should exist at " + expectedFile);

        //Dependency Package
        DownloadServiceWorkData workdata2 = new DownloadServiceWorkData(downloadUrl,
                targetLocation.toString(), "1.0.0", project, false, inventoryItemId);
        result = downloadService.process(workdata2);
        assertTrue(result);
        expectedFile = targetLocation.resolve("dependencies").resolve("dummy-repo")
                .resolve("1.0.0")
                .resolve("dummy-project").resolve("dummy-main").resolve("dummy-file.txt");

        assertTrue(Files.exists(expectedFile), "Dependency file should exist in /dependencies folder");
    }

    @Test
    void testProcessValidPathGit() throws IOException, InterruptedException {
        String project = UUID.randomUUID().toString();
        String inventoryItemId = UUID.randomUUID().toString();
        String gitUrl = "git+https://github.com/fake/repo.git";
        String resolvedZipUrl = "http://localhost:" + localPort + "/dummy-repo.zip";
        Path targetLocation = tempDir.resolve("git_test_root");

        DownloadServiceWorkData workData = new DownloadServiceWorkData(
                gitUrl, targetLocation.toString(), "v1.0", project, true, inventoryItemId
        );

        when(projectRepository.findById(any())).thenReturn(Optional.of(new Project()));
        when(inventoryItemRepository.findById(any())).thenReturn(Optional.of(new InventoryItem()));
        when(gitRepoController.getGitRepository("fake", "repo", "v1.0")).thenReturn(resolvedZipUrl);

        boolean result = downloadService.process(workData);
        assertTrue(result);
        verify(gitRepoController).getGitRepository("fake", "repo", "v1.0");
        verify(fileService).createEntitiesFromPath(any(), any(), any(), eq(true));
    }

    @Test
    void testProcessInvalidOrUnsupportedPath() {
        String project = UUID.randomUUID().toString();
        String inventoryItemId = UUID.randomUUID().toString();
        // SVN unsupported
        DownloadServiceWorkData workData = new DownloadServiceWorkData(
                "svn://svn.apache.org/repos/asf", tempDir.toString(), "1.0", project, true, inventoryItemId
        );

        when(projectRepository.findById(any())).thenReturn(Optional.of(new Project()));
        when(inventoryItemRepository.findById(any())).thenReturn(Optional.of(new InventoryItem()));
        boolean result = downloadService.process(workData);
        assertTrue(result, "Should return true (handled) even if skipped");
        verifyNoInteractions(fileService);
        verifyNoInteractions(gitRepoController);
    }
}
