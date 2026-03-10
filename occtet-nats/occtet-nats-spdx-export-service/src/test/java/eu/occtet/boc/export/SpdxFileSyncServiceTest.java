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

import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.spdxV2.RelationshipEntity;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxFileEntity;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpdxFileSyncServiceTest {

    @InjectMocks
    private SpdxFileSyncService spdxFileSyncService;

    @Mock
    private FileRepository fileRepository;

    private Project project;
    private InventoryItem inventoryItem;
    private SpdxPackageEntity packageEntity;
    private SpdxDocumentRoot documentRoot;

    @BeforeEach
    void setUp() {
        project = new Project("Test Project");
        inventoryItem = new InventoryItem("LibNode", project, null);

        packageEntity = new SpdxPackageEntity();
        packageEntity.setSpdxId("SPDXRef-Package-1");

        documentRoot = new SpdxDocumentRoot();
        documentRoot.setSpdxId("SPDXRef-DOCUMENT");
        documentRoot.setRelationships(new ArrayList<>());
    }

    @Test
    void synchronizeFiles_WithSomeFilesDeleted_RemovesStaleRelationships() {
        SpdxFileEntity keptFileEntity = new SpdxFileEntity();
        keptFileEntity.setSpdxId("SPDXRef-File-Keep");

        SpdxFileEntity deletedFileEntity = new SpdxFileEntity();
        deletedFileEntity.setSpdxId("SPDXRef-File-Deleted");

        documentRoot.getFiles().add(keptFileEntity);
        documentRoot.getFiles().add(deletedFileEntity);

        RelationshipEntity rel1 = new RelationshipEntity();
        rel1.setSpdxElementId("SPDXRef-Package-1");
        rel1.setRelatedSpdxElement("SPDXRef-File-Keep");
        rel1.setRelationshipType("CONTAINS");

        RelationshipEntity rel2 = new RelationshipEntity();
        rel2.setSpdxElementId("SPDXRef-Package-1");
        rel2.setRelatedSpdxElement("SPDXRef-File-Deleted");
        rel2.setRelationshipType("CONTAINS");

        documentRoot.getRelationships().add(rel1);
        documentRoot.getRelationships().add(rel2);

        File keptFile = new File(inventoryItem, "/src/Keep.java", project);
        keptFile.setDocumentId("SPDXRef-File-Keep");

        when(fileRepository.findByInventoryItemsContaining(inventoryItem)).thenReturn(List.of(keptFile));

        spdxFileSyncService.synchronizeFiles(packageEntity, inventoryItem, documentRoot);

        assertEquals(1, packageEntity.getFileNames().size(), "Package should only contain 1 file");
        assertEquals("SPDXRef-File-Keep", packageEntity.getFileNames().getFirst());

        assertEquals(1, documentRoot.getRelationships().size(), "Stale relationship should be removed");
        assertEquals("SPDXRef-File-Keep", documentRoot.getRelationships().getFirst().getRelatedSpdxElement());

        assertEquals(1, documentRoot.getFiles().size(), "Orphaned file entity should be removed from the document");
        assertEquals("SPDXRef-File-Keep", documentRoot.getFiles().getFirst().getSpdxId());
    }

    @Test
    void synchronizeFiles_WithAllFilesDeleted_ClearsFilesAndRelationships() {
        SpdxFileEntity deletedFile = new SpdxFileEntity();
        deletedFile.setSpdxId("SPDXRef-File-Deleted");
        documentRoot.getFiles().add(deletedFile);

        RelationshipEntity rel1 = new RelationshipEntity();
        rel1.setSpdxElementId("SPDXRef-Package-1");
        rel1.setRelatedSpdxElement("SPDXRef-File-Deleted");
        rel1.setRelationshipType("CONTAINS");
        documentRoot.getRelationships().add(rel1);

        packageEntity.setFileNames(new ArrayList<>(List.of("SPDXRef-File-Deleted")));
        packageEntity.setFilesAnalyzed(true);

        when(fileRepository.findByInventoryItemsContaining(inventoryItem)).thenReturn(new ArrayList<>());

        spdxFileSyncService.synchronizeFiles(packageEntity, inventoryItem, documentRoot);

        assertTrue(packageEntity.getFileNames().isEmpty(), "File names list should be cleared");
        assertFalse(packageEntity.getFilesAnalyzed(), "Files analyzed flag should be set to false");
        assertTrue(documentRoot.getRelationships().isEmpty(), "Relationships should be purged");
        assertTrue(documentRoot.getFiles().isEmpty(), "Orphaned files should be removed");
    }
}