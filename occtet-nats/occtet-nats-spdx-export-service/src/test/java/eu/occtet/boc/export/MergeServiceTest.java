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

package eu.occtet.boc.export;

import eu.occtet.boc.config.TestEclipseLinkJpaConfiguration;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.SpdxDocumentRootRepository;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Organization;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.spdxV2.CreationInfoEntity;
import eu.occtet.boc.entity.spdxV2.PackageVerificationCodeEntity;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.export.service.MergeService;
import eu.occtet.boc.export.service.SpdxFileSyncService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        MergeService.class, TestEclipseLinkJpaConfiguration.class
})
@EnableJpaRepositories(basePackages = {"eu.occtet.boc.dao"})
@EntityScan(basePackages = {"eu.occtet.boc.entity"})
public class MergeServiceTest {

    @Autowired
    private MergeService mergeService;

    @MockitoBean
    private InventoryItemRepository inventoryItemRepository;

    @MockitoBean
    private SpdxDocumentRootRepository spdxDocumentRootRepository;

    @MockitoBean
    private SpdxFileSyncService spdxFileSyncService;

    @Test
    void mergeChangesToDocumentEntities_WithCuratedComponent_UpdatesPackageEntity() {
        Project project = new Project("Test Project");
        project.setVersion("1.0.0");
        project.setProjectContact("Jane Doe");
        project.setCreatedAt(LocalDateTime.now());

        Organization org = new Organization();
        org.setOrganizationEmail("test@test.com");
        org.setOrganizationName("Test Org");

        project.setOrganization(org);
        org.getProjects().add(project);

        SpdxDocumentRoot documentRoot = getSpdxDocumentRoot();

        PackageVerificationCodeEntity verificationCode = new PackageVerificationCodeEntity();
        verificationCode.setPackageVerificationCodeValue("0000000000000000000000000000000000000000");

        SpdxPackageEntity documentPackage = new SpdxPackageEntity();
        documentPackage.setSpdxId("SPDXRef-Package-1");
        documentPackage.setName("old-name");
        documentPackage.setDownloadLocation("NOASSERTION");
        documentPackage.setVersionInfo("1.0");
        documentPackage.setPackageVerificationCodeEntity(verificationCode);
        documentPackage.setSpdxDocument(documentRoot);

        documentRoot.getPackages().add(documentPackage);

        SoftwareComponent curatedComponent = new SoftwareComponent("updated-name", "2.0", new ArrayList<>(), org);
        curatedComponent.setCurated(true);
        curatedComponent.setDetailsUrl("https://new-location.com");

        InventoryItem item = new InventoryItem("InventoryNode", project, curatedComponent);
        item.setCurated(true);
        item.setSpdxId("SPDXRef-Package-1");

        when(inventoryItemRepository.findBySpdxIdAndProject("SPDXRef-Package-1", project))
                .thenReturn(List.of(item));

        mergeService.mergeChangesToDocumentEntities(documentRoot, project);

        assertEquals("updated-name", documentPackage.getName());
        assertEquals("2.0", documentPackage.getVersionInfo());
        assertEquals("https://new-location.com", documentPackage.getDownloadLocation());
        assertEquals("Test Project", documentRoot.getName()); // Mergeservice overrides doc name with project name

        verify(spdxFileSyncService, times(1)).synchronizeFiles(eq(documentPackage), eq(item), eq(documentRoot));
        verify(spdxDocumentRootRepository, times(1)).save(documentRoot);
    }

    @NotNull
    private static SpdxDocumentRoot getSpdxDocumentRoot() {
        CreationInfoEntity creationInfo = new CreationInfoEntity();
        creationInfo.setCreated("2026-03-09T10:00:00Z");
        creationInfo.setCreators("Tool: TestTool");

        SpdxDocumentRoot documentRoot = new SpdxDocumentRoot();
        documentRoot.setSpdxId("SPDXRef-DOCUMENT");
        documentRoot.setSpdxVersion("SPDX-2.3");
        documentRoot.setDataLicense("CC0-1.0");
        documentRoot.setName("Old Document Name");
        documentRoot.setDocumentUri("https://test.uri/doc");
        documentRoot.setCreationInfo(creationInfo);
        return documentRoot;
    }

    @Test
    void mergeChangesToDocumentEntities_ComponentNotCurated_SkipsUpdate() {
        Project project = new Project("Test Project");

        SpdxDocumentRoot documentRoot = new SpdxDocumentRoot();
        documentRoot.setSpdxId("SPDXRef-DOCUMENT");

        SpdxPackageEntity documentPackage = new SpdxPackageEntity();
        documentPackage.setSpdxId("SPDXRef-Package-1");
        documentPackage.setName("original-name");
        documentRoot.getPackages().add(documentPackage);

        SoftwareComponent uncuratedComponent = new SoftwareComponent("updated-name", "2.0", new ArrayList<>(), new Organization());
        uncuratedComponent.setCurated(false); // Flag is explicitly false

        InventoryItem item = new InventoryItem("InventoryNode", project, uncuratedComponent);
        item.setCurated(false);
        item.setSpdxId("SPDXRef-Package-1");

        when(inventoryItemRepository.findBySpdxIdAndProject("SPDXRef-Package-1", project))
                .thenReturn(List.of(item));

        mergeService.mergeChangesToDocumentEntities(documentRoot, project);

        assertEquals("original-name", documentPackage.getName());
        verify(spdxFileSyncService, never()).synchronizeFiles(any(), any(), any());
    }

    @Test
    void mergeChangesToDocumentEntities_InventoryItemNotFound_CatchesExceptionAndContinues() {
        Project project = new Project("Test Project");

        SpdxDocumentRoot documentRoot = new SpdxDocumentRoot();

        SpdxPackageEntity documentPackage = new SpdxPackageEntity();
        documentPackage.setSpdxId("SPDXRef-Orphan-Package");
        documentRoot.getPackages().add(documentPackage);

        when(inventoryItemRepository.findBySpdxIdAndProject("SPDXRef-Orphan-Package", project))
                .thenReturn(new ArrayList<>());

        mergeService.mergeChangesToDocumentEntities(documentRoot, project);

        verify(spdxDocumentRootRepository, times(1)).save(documentRoot);
    }
}
