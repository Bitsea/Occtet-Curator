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

package eu.occtet.boc.download.service;

import com.github.packageurl.PackageURL;
import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.download.factory.DownloadStrategyFactory;
import eu.occtet.boc.download.strategies.DownloadStrategy;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
import eu.occtet.boc.model.DownloadServiceWorkData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DownloadManagerTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private AppConfigurationRepository appConfigurationRepository;
    @Mock private DownloadStrategyFactory downloadStrategyFactory;
    @Mock private ArchiveService archiveService;
    @Mock private FileService fileService;

    @Mock private DownloadStrategy strategyA;
    @Mock private DownloadStrategy strategyB;

    @InjectMocks
    private DownloadManager downloadManager;

    @TempDir
    Path tempDir;

    private Project project;
    private InventoryItem inventoryItem;
    private SoftwareComponent softwareComponent;
    private DownloadServiceWorkData workData;

    @BeforeEach
    void setup() {
        AppConfiguration config = new AppConfiguration();
        config.setValue(tempDir.toString());
        lenient().when(appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH))
                .thenReturn(Optional.of(config));

        project = new Project();
        project.setId(101L);
        project.setProjectName("TestProject");

        softwareComponent = new SoftwareComponent();
        softwareComponent.setId(1L);
        softwareComponent.setName("User-Defined-Name");
        softwareComponent.setVersion("1.0.0");
        softwareComponent.setDetailsUrl("https://github.com/test/repo");
        softwareComponent.setPurl("pkg:maven/org.test/clean-lib-name@1.0.0");

        inventoryItem = new InventoryItem();
        inventoryItem.setId(500L);
        inventoryItem.setSoftwareComponent(softwareComponent);
        inventoryItem.setInventoryName("InvItem1");

        workData = new DownloadServiceWorkData(project.getId(),inventoryItem.getId(),false);

        lenient().when(projectRepository.findById(101L)).thenReturn(Optional.of(project));
        lenient().when(inventoryItemRepository.findById(500L)).thenReturn(Optional.of(inventoryItem));
    }

    @Test
    void testProcess_SuccessfulUrlDownload_WithCanonicalName() throws Exception {
        Path dummyDownload = Files.createFile(tempDir.resolve("downloaded.zip"));

        when(downloadStrategyFactory.findForUrl(any(URL.class), anyString()))
                .thenReturn(List.of(strategyA));

        when(strategyA.download(any(URL.class), eq("1.0.0"), any(Path.class)))
                .thenReturn(dummyDownload);

        boolean result = downloadManager.process(workData);

        assertTrue(result);

        ArgumentCaptor<Path> targetPathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(archiveService).unpack(eq(dummyDownload), targetPathCaptor.capture());

        Path usedPath = targetPathCaptor.getValue();
        // Should use PURL name ("clean-lib-name") instead of component name
        assertTrue(usedPath.toString().contains("clean-lib-name"));
        assertFalse(usedPath.toString().contains("User-Defined-Name"));

        verify(fileService).createEntitiesFromPath(eq(project), any(Path.class), anyString());

        assertFalse(Files.exists(dummyDownload));
    }

    @Test
    void testProcess_UrlFails_PurlSucceeds() throws Exception {
        Path dummyDownload = Files.createFile(tempDir.resolve("purl_download.zip"));

        when(downloadStrategyFactory.findForUrl(any(URL.class), anyString()))
                .thenReturn(List.of(strategyA));

        when(downloadStrategyFactory.findForPurl(any(PackageURL.class)))
                .thenReturn(List.of(strategyB));

        when(strategyA.download(any(URL.class), anyString(), any(Path.class)))
                .thenThrow(new IOException("404 Not Found"));

        when(strategyB.download(any(PackageURL.class), any(Path.class)))
                .thenReturn(dummyDownload);

        boolean result = downloadManager.process(workData);

        assertTrue(result);

        verify(strategyA).download(any(URL.class), anyString(), any(Path.class));
        verify(strategyB).download(any(PackageURL.class), any(Path.class));
        verify(archiveService).unpack(eq(dummyDownload), any(Path.class));
    }

    @Test
    void testProcess_AllStrategiesFail() throws Exception {
        when(downloadStrategyFactory.findForUrl(any(), any())).thenReturn(List.of(strategyA));
        when(downloadStrategyFactory.findForPurl(any())).thenReturn(List.of(strategyB));
        when(downloadStrategyFactory.findForName(any(), any())).thenReturn(Collections.emptyList());

        when(strategyA.download(any(URL.class), anyString(), any(Path.class)))
                .thenThrow(new IOException("URL Failed"));
        when(strategyB.download(any(PackageURL.class), any(Path.class)))
                .thenThrow(new IOException("PURL Failed"));

        boolean result = downloadManager.process(workData);

        assertFalse(result);

        verify(archiveService, never()).unpack(any(), any());

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemRepository).save(itemCaptor.capture());

        InventoryItem savedItem = itemCaptor.getValue();
        assertNotNull(savedItem.getExternalNotes());
        assertTrue(savedItem.getExternalNotes().contains("WARNING: Unable to download"));
    }

    @Test
    void testProcess_InvalidProject_ReturnsFalse() {
        when(projectRepository.findById(101L)).thenReturn(Optional.empty());

        boolean result = downloadManager.process(workData);

        assertFalse(result);
        verifyNoInteractions(downloadStrategyFactory);
    }
}