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
import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.download.controller.GitRepoController;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.download.service.DownloadService;
import eu.occtet.boc.download.service.FileService;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
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
    @Mock
    private AppConfigurationRepository appConfigurationRepository;
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
        Path rootPath = tempDir.resolve("project_temp_root");
        AppConfiguration mockAppConfig = new AppConfiguration();
        mockAppConfig.setValue(rootPath.toString());

        when(appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH))
                .thenReturn(Optional.of(mockAppConfig));

        Long projectId = 12345L;
        Long inventoryItemId = 789L;
        String downloadUrl = "http://localhost:" + localPort + "/dummy-repo.zip";

        Project mockProject = new Project();
        mockProject.setId(projectId);
        mockProject.setProjectName("my-target-project");

        SoftwareComponent mockComponent = new SoftwareComponent();
        mockComponent.setVersion("1.0.0");
        mockComponent.setName("dummy-main");

        InventoryItem mockItem = new InventoryItem();
        mockItem.setSoftwareComponent(mockComponent);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(inventoryItemRepository.findById(inventoryItemId)).thenReturn(Optional.of(mockItem));


        // Main package
        DownloadServiceWorkData workData = new DownloadServiceWorkData(downloadUrl, projectId, inventoryItemId, true);
        boolean result = downloadService.process(workData);
        assertTrue(result);

        // Logic: [Base] / [ProjectName] / [RepoName] / [Version] / [InternalZipPath]
        Path expectedFile = rootPath
                .resolve("my-target-project_" + projectId)  // From project.getProjectName()
                .resolve("dummy-repo")         // From URL
                .resolve("1.0.0")              // From Version
                .resolve("dummy-project")      // Inside ZIP
                .resolve("dummy-main")         // Inside ZIP
                .resolve("dummy-file.txt");    // Inside ZIP

        assertTrue(Files.exists(expectedFile), "Main package file should exist at " + expectedFile);

        // Dependency Package
        DownloadServiceWorkData workdata2 = new DownloadServiceWorkData(downloadUrl, projectId, inventoryItemId, false);
        result = downloadService.process(workdata2);
        assertTrue(result);

        // Logic: [Base] / dependencies / [RepoName] / [Version] / ...
        Path expectedDepFile = rootPath
                .resolve("my-target-project_" + projectId)
                .resolve("dependencies")
                .resolve("dummy-repo")
                .resolve("1.0.0")
                .resolve("dummy-project")
                .resolve("dummy-main")
                .resolve("dummy-file.txt");

        assertTrue(Files.exists(expectedDepFile));
    }

    @Test
    void testProcessValidPathGit() throws IOException, InterruptedException {
        Path rootPath = tempDir.resolve("project_temp_root");

        AppConfiguration mockAppConfig = new AppConfiguration();
        mockAppConfig.setValue(rootPath.toString());
        when(appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH))
                .thenReturn(Optional.of(mockAppConfig));

        Long projectId = 12345L;
        Long inventoryItemId = 789L;

        // INPUT URL (Must start with git+ to trigger controller)
        String gitUrl = "git+https://github.com/fake/dummy-repo.git";
        // RESOLVED URL (Points to our local server)
        String resolvedZipUrl = "http://localhost:" + localPort + "/dummy-repo.zip";

        Project mockProject = new Project();
        mockProject.setId(projectId);
        mockProject.setProjectName("git-project");

        SoftwareComponent mockComponent = new SoftwareComponent();
        mockComponent.setVersion("1.0.0");
        mockComponent.setName("dummy-main");

        InventoryItem mockItem = new InventoryItem();
        mockItem.setSoftwareComponent(mockComponent);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(inventoryItemRepository.findById(inventoryItemId)).thenReturn(Optional.of(mockItem));

        when(gitRepoController.resolveGitToZipUrl("fake", "dummy-repo", "1.0.0")).thenReturn(resolvedZipUrl);

        DownloadServiceWorkData workData = new DownloadServiceWorkData(gitUrl, projectId, inventoryItemId, true);
        boolean result = downloadService.process(workData);
        assertTrue(result);

        Path expectedFile = rootPath
                .resolve("git-project_" + projectId)
                .resolve("dummy-repo")      // From Git URL repo name
                .resolve("1.0.0")
                .resolve("dummy-project")   // Inside Zip
                .resolve("dummy-main")      // Inside Zip
                .resolve("dummy-file.txt");

        assertTrue(Files.exists(expectedFile), "Git downloaded file should exist at " + expectedFile);

        verify(gitRepoController).resolveGitToZipUrl("fake", "dummy-repo", "1.0.0");
    }

    @Test
    void testProcessInvalidOrUnsupportedPath() {
        AppConfiguration mockAppConfig = new AppConfiguration();
        mockAppConfig.setValue("/tmp/test-base-path");
        when(appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH))
                .thenReturn(Optional.of(mockAppConfig));

        Long project = 654L;
        Long inventoryItemId = 8745L;
        String unsupportedURL = "svn://svn.apache.org/repos/asf";

        DownloadServiceWorkData workData = new DownloadServiceWorkData(
                unsupportedURL, project, inventoryItemId, false
        );

        SoftwareComponent mockComponent = new SoftwareComponent();
        mockComponent.setVersion("1.0.0");
        mockComponent.setName("dummy-main");

        InventoryItem mockItem = new InventoryItem();
        mockItem.setSoftwareComponent(mockComponent);

        when(projectRepository.findById(any())).thenReturn(Optional.of(new Project()));
        when(inventoryItemRepository.findById(any())).thenReturn(Optional.of(mockItem));

        boolean result = downloadService.process(workData);

        assertTrue(result, "Should return true (handled) even if skipped");
        verifyNoInteractions(fileService);
        verifyNoInteractions(gitRepoController);
    }
}
