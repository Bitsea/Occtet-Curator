/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.spdx.utlities;

import eu.occtet.boc.config.TestEclipseLinkJpaConfiguration;
import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.spdx.context.SpdxImportContext;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.factory.*;
import eu.occtet.boc.spdx.service.*;
import eu.occtet.boc.spdx.service.handler.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {SpdxService.class, SoftwareComponentService.class, SoftwareComponentRepository.class,
        CopyrightService.class, InventoryItemService.class, LicenseService.class, FileService.class,
        ProjectRepository.class, LicenseRepository.class, InventoryItemRepository.class, SoftwareComponentFactory.class, FileRepository.class,
        CopyrightFactory.class, FileFactory.class, InventoryItemFactory.class, CleanUpService.class,
        LicenseFactory.class, SpdxConverter.class, TestEclipseLinkJpaConfiguration.class, LicenseHandler.class, PackageHandler.class, OrphanHandler.class,
        RelationshipHandler.class, SnippetHandler.class
})
@EnableJpaRepositories(basePackages = {
        "eu.occtet.boc.dao"})
@EntityScan(basePackages = {
        "eu.occtet.boc.entity"
})
@ExtendWith(MockitoExtension.class)


public class SpdxServiceTest {

    private static final String TEST_FILE = "synthetic-scan-result-expected-output.spdx.json";

    @Autowired
    private SpdxService spdxService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @MockitoBean
    private PackageHandler packageHandler;
    @MockitoBean
    private RelationshipHandler relationshipHandler;
    @MockitoBean
    private SnippetHandler snippetHandler;
    @MockitoBean
    private OrphanHandler orphanHandler;
    @MockitoBean
    private SpdxConverter spdxConverter;
    @MockitoBean
    private AnswerService answerService;
    @MockitoBean
    private SpdxDocumentRootRepository spdxDocumentRootRepository;
    @MockitoBean
    private CleanUpService cleanUpService;

    private Project project;
    private byte[] jsonBytes;

    @BeforeEach
    public void setup() throws IOException {
        project = new Project();
        project.setProjectName("Orchestration Project");
        project = projectRepository.save(project);

        jsonBytes = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(TEST_FILE).readAllBytes();

        Mockito.lenient().when(spdxConverter.convertSpdxV2DocumentInformation(any()))
                .thenReturn(new SpdxDocumentRoot());
        Mockito.lenient().when(spdxDocumentRootRepository.save(any()))
                .thenReturn(new SpdxDocumentRoot());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testParseDocument_OrchestrationFlow() throws Exception {
        SpdxWorkData workData = new SpdxWorkData();
        workData.setProjectId(project.getId());
        workData.setJsonBytes(jsonBytes);
        workData.setUseCopyrightAi(false);
        workData.setUseLicenseMatcher(false);

        InventoryItem simulatedItem = new InventoryItem();
        simulatedItem.setProject(project);
        simulatedItem.setSpdxId("SPDXRef-Package-Maven-pkg7-grp-pkg7-0.0.1-source-artifact");
        simulatedItem.setInventoryName("Simulated Item");
        inventoryItemRepository.save(simulatedItem);

        boolean result = spdxService.process(workData);


        Assertions.assertTrue(result, "Service should return true on success");

        org.mockito.InOrder inOrder = Mockito.inOrder(
                cleanUpService,
                packageHandler,
                orphanHandler,
                relationshipHandler,
                snippetHandler
        );

        inOrder.verify(cleanUpService).cleanUpFileTree(any(Project.class));
        inOrder.verify(packageHandler).processAllPackages(any(SpdxImportContext.class), any(Consumer.class));
        inOrder.verify(orphanHandler).processOrphanFiles(any(SpdxImportContext.class));
        inOrder.verify(relationshipHandler).processAllRelationships(any(SpdxImportContext.class), any(Consumer.class));
        inOrder.verify(snippetHandler).processAllSnippets(any(SpdxImportContext.class));
    }

    @Test
    public void testParseDocument_ContextPopulation() throws Exception {

        SpdxWorkData workData = new SpdxWorkData();
        workData.setProjectId(project.getId());
        workData.setJsonBytes(jsonBytes);

        // Simulate DB state for cache refresh
        InventoryItem dbItem = new InventoryItem();
        dbItem.setProject(project);
        dbItem.setSpdxId("SPDXRef-Existing-Item");
        dbItem.setInventoryName("Existing Item");
        inventoryItemRepository.save(dbItem);

        spdxService.process(workData);

        ArgumentCaptor<SpdxImportContext> contextCaptor = ArgumentCaptor.forClass(SpdxImportContext.class);
        Mockito.verify(relationshipHandler).processAllRelationships(contextCaptor.capture(), any());

        SpdxImportContext context = contextCaptor.getValue();


        Assertions.assertTrue(context.getMainPackageIds().contains("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1"),
                "Context should contain Main Package IDs extracted from DocumentDescribes");
        Assertions.assertTrue(context.getInventoryCache().containsKey("SPDXRef-Existing-Item"),
                "Context InventoryCache should be populated with items found in DB");
        Assertions.assertEquals(dbItem.getId(), context.getInventoryCache().get("SPDXRef-Existing-Item").getId());
    }

    @Test
    public void testParseDocument_InvalidProject_ReturnsFalse() {
        SpdxWorkData workData = new SpdxWorkData();
        workData.setProjectId(999999L); // Non-existent ID
        workData.setJsonBytes(jsonBytes);

        boolean result = spdxService.process(workData);

        Assertions.assertFalse(result, "Should return false if project does not exist");

        try {
            Mockito.verify(packageHandler, Mockito.never()).processAllPackages(any(), any());
        } catch (Exception e) {
            Assertions.fail("Mock verification failed");
        }
    }
}
