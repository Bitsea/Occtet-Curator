/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.export;

import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.SpdxDocumentRootRepository;
import eu.occtet.boc.dao.SpdxPackageRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.export.service.MergeService;
import eu.occtet.boc.export.service.SpdxFileSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private SpdxPackageRepository spdxPackageRepository;

    @Mock
    private SpdxDocumentRootRepository spdxDocumentRootRepository;

    @Mock
    private SpdxFileSyncService spdxFileSyncService;

    @InjectMocks
    private MergeService mergeService;

    private Project project;
    private SpdxDocumentRoot documentRoot;

    @BeforeEach
    void setUp() {
        project = new Project("Test Project");
        project.setId(100L);

        documentRoot = new SpdxDocumentRoot();
        documentRoot.setPackages(new ArrayList<>());
        documentRoot.setFiles(new ArrayList<>());
        documentRoot.setHasExtractedLicensingInfos(new ArrayList<>());
        documentRoot.setRelationships(new ArrayList<>());
    }

    @Test
    void mergeChangesToDocumentEntities_WithCuratedItems_SuccessfullyTransformsToSpdx() {
        // Setup: Source of Truth
        Copyright copyrightAlice = new Copyright();
        copyrightAlice.setCopyrightText("Copyright (C) 2026 Alice");

        License customLicenseTemplate = new License();
        customLicenseTemplate.setLicenseType("Custom1");
        customLicenseTemplate.setLicenseName("Custom-MIT-Derivative");
        customLicenseTemplate.setIsSpdx(false);

        SoftwareComponentLicenseUsage licenseUsage = new SoftwareComponentLicenseUsage();
        licenseUsage.setTemplate(customLicenseTemplate);
        licenseUsage.setUsageText("Original License Text");
        licenseUsage.setIsModified(false);

        SoftwareComponent component = new SoftwareComponent();
        component.setName("jackson-databind");
        component.setVersion("2.15.2");
        component.setDetailsUrl("https://github.com/fasterxml/jackson-databind");
        component.setCopyrights(List.of(copyrightAlice));
        component.setUsageLicenses(List.of(licenseUsage));
        component.setCurated(true);

        licenseUsage.setSoftwareComponent(component);

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setId(1L);
        inventoryItem.setInventoryName("jackson-databind-2.15.2");
        inventoryItem.setSoftwareComponent(component);
        inventoryItem.setCurated(true);

        List<InventoryItem> mockInventory = List.of(inventoryItem);


        when(inventoryItemRepository.findAllByProjectAndCurated(project, true))
                .thenReturn(mockInventory);


        when(spdxPackageRepository.save(any(SpdxPackageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(spdxDocumentRootRepository.save(any(SpdxDocumentRoot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mergeService.mergeChangesToDocumentEntities(documentRoot, project, true);


        assertEquals(1, documentRoot.getPackages().size(), "Es sollte genau ein Package erzeugt werden");
        SpdxPackageEntity generatedPkg = documentRoot.getPackages().getFirst();

        assertEquals("jackson-databind", generatedPkg.getName());
        assertEquals("2.15.2", generatedPkg.getVersionInfo());
        assertEquals("https://github.com/fasterxml/jackson-databind", generatedPkg.getHomepage());
        assertEquals("Copyright (C) 2026 Alice", generatedPkg.getCopyrightText());


        assertEquals("SPDXRef-Package-jackson-databind-2.15.2", inventoryItem.getSpdxId(), "Die SpdxId am InventoryItem sollte gesetzt worden sein");


        assertNotNull(documentRoot.getHasExtractedLicensingInfos());
        assertFalse(documentRoot.getHasExtractedLicensingInfos().isEmpty());


        verify(spdxFileSyncService, times(1))
                .synchronizeFiles(eq(generatedPkg), eq(inventoryItem), eq(documentRoot));

        verify(spdxDocumentRootRepository, times(1)).save(documentRoot);
    }

    @Test
    void mergeChangesToDocumentEntities_WithNonCuratedItem_ShouldSkipItem() {
        InventoryItem nonCuratedItem = new InventoryItem();
        nonCuratedItem.setCurated(false);

        when(inventoryItemRepository.findAllByProjectAndCurated(project, true))
                .thenReturn(List.of(nonCuratedItem));

        mergeService.mergeChangesToDocumentEntities(documentRoot, project, true);

        assertTrue(documentRoot.getPackages().isEmpty(), "not curated inventoryItems should not be handled");
    }
}