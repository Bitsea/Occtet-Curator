/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.spdx.utlities;

import eu.occtet.boc.config.TestEclipseLinkJpaConfiguration;
import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.RelationshipEntity;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxFileEntity;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.spdx.context.SpdxImportContext;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.factory.*;
import eu.occtet.boc.spdx.service.*;
import eu.occtet.boc.spdx.service.handler.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        SpdxService.class, SoftwareComponentService.class, SoftwareComponentRepository.class,
        CopyrightService.class, InventoryItemService.class, LicenseService.class, FileService.class,
        ProjectRepository.class, LicenseRepository.class, InventoryItemRepository.class, SoftwareComponentFactory.class, FileRepository.class,
        CopyrightFactory.class, FileFactory.class, InventoryItemFactory.class, CleanUpService.class,
        LicenseFactory.class, SpdxConverter.class, TestEclipseLinkJpaConfiguration.class,
        LicenseHandler.class, PackageHandler.class, OrphanHandler.class, RelationshipHandler.class, SnippetHandler.class
})
@EnableJpaRepositories(basePackages = {"eu.occtet.boc.dao"})
@EntityScan(basePackages = {"eu.occtet.boc.entity"})
@ExtendWith(MockitoExtension.class)
@Transactional
public class SpdxHandlerTest {

    private static final String TEST_FILE_NAME = "synthetic-scan-result-expected-output.spdx.json";

    @MockitoBean
    private AnswerService answerService;
    @MockitoBean
    private SpdxConverter spdxConverter;
    @MockitoBean
    private SpdxDocumentRootRepository spdxDocumentRootRepository;
    @MockitoBean
    private CleanUpService cleanUpService;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseHandler licenseHandler;
    @Autowired
    private PackageHandler packageHandler;
    @Autowired
    private RelationshipHandler relationshipHandler;
    @Autowired
    private OrphanHandler orphanHandler;
    @Autowired
    private SnippetHandler snippetHandler;

    private Project project;
    private SpdxImportContext context;

    @BeforeEach
    public void setup() throws Exception {
        project = new Project();
        project.setProjectName("IntegrationTestProject");
        project = projectRepository.save(project);

        SpdxModelFactory.init();
        MultiFormatStore inputStore = new MultiFormatStore(new InMemSpdxStore(), MultiFormatStore.Format.JSON);

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_FILE_NAME);
        if (is == null) {
            throw new RuntimeException("Test file not found: " + TEST_FILE_NAME);
        }
        SpdxDocument spdxDocument = inputStore.deSerialize(new ByteArrayInputStream(is.readAllBytes()), false);

        SpdxDocumentRoot root = new SpdxDocumentRoot();
        context = new SpdxImportContext(project, spdxDocument, root);
        context.setExtractedLicenseInfos(spdxDocument.getExtractedLicenseInfos());

        Mockito.when(spdxConverter.convertPackage(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new SpdxPackageEntity());
        Mockito.when(spdxConverter.convertFile(Mockito.any(), Mockito.any()))
                .thenReturn(new SpdxFileEntity());
        Mockito.when(spdxConverter.convertRelationShip(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new RelationshipEntity());
    }

    @Test
    public void testPackageHandler_ShouldProcessSpecificPackage() throws Exception {
        packageHandler.processAllPackages(context, (p) -> {});

        String targetSpdxId = "SPDXRef-Package-Maven-pkg7-grp-pkg7-0.0.1-source-artifact";
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findBySpdxIdAndProject(targetSpdxId, project).stream().findFirst();

        Assertions.assertTrue(itemOpt.isPresent(), "Pkg7 source artifact should be created");
        InventoryItem item = itemOpt.get();

        Assertions.assertTrue(item.getInventoryName().contains("pkg7-grp-pkg7-0.0.1-source-artifact"));
        Assertions.assertTrue(item.getInventoryName().contains("GPL-2.0-only WITH NOASSERTION"));

        SoftwareComponent component = item.getSoftwareComponent();
        Assertions.assertEquals("pkg7", component.getName());
        Assertions.assertEquals("0.0.1", component.getVersion());
        Assertions.assertEquals("https://example.com/pkg7-sources.jar", component.getDetailsUrl());

        Assertions.assertTrue(component.getCopyrights().stream()
                        .anyMatch(c -> c.getCopyrightText().equals("Copyright 2020 Some copyright holder in source artifact")),
                "Should extract copyright from pkg7");


        InventoryItem pkg7Item = inventoryItemRepository.findBySpdxIdAndProject(
                        "SPDXRef-Package-Maven-pkg7-grp-pkg7-0.0.1-source-artifact", project)
                .stream().findFirst().orElseThrow();

        SoftwareComponent pkg7Comp = pkg7Item.getSoftwareComponent();
        Assertions.assertEquals("pkg:maven/pkg7-grp/pkg7@0.0.1", pkg7Comp.getPurl(),
                "Should extract PURL from ExternalRefs");

        Assertions.assertFalse(pkg7Item.getWasCombined(),
                "Should be false because 'WITH' is not treated as a combination operator in the regex");

        InventoryItem goItem = inventoryItemRepository.findBySpdxIdAndProject(
                        "SPDXRef-Package-Go-gopkg.in.yaml.v3-3.0.1", project)
                .stream().findFirst().orElseThrow();

        Assertions.assertTrue(goItem.getWasCombined(),
                "Should be true because license string contains 'AND'");

        Assertions.assertEquals("pkg:golang/gopkg.in/yaml.v3@3.0.1", goItem.getSoftwareComponent().getPurl(),
                "Should extract Golang PURL correctly");
    }


    @Test
    public void testOrphanHandler_ShouldFindSpecificOrphanFile() throws Exception {
        packageHandler.processAllPackages(context, (p) -> {});


        orphanHandler.processOrphanFiles(context);


        String orphanId = "SPDXRef-OrphanedFile1";
        List<InventoryItem> orphans = inventoryItemRepository.findBySpdxIdAndProject(orphanId, project);

        Assertions.assertFalse(orphans.isEmpty(), "Orphaned file should be detected");
        InventoryItem orphanItem = orphans.getFirst();

        Assertions.assertEquals("some/file", orphanItem.getInventoryName());
        Assertions.assertEquals(1, orphanItem.getSize());

        SoftwareComponent comp = orphanItem.getSoftwareComponent();
        Assertions.assertEquals("Standalone", comp.getVersion());

        boolean hasCopyright = comp.getCopyrights().stream()
                .anyMatch(c -> c.getCopyrightText().equals("Copyright 2020 Some copyright holder in source artifact"));
        Assertions.assertTrue(hasCopyright, "Orphan copyright should be extracted");
    }


    @Test
    public void testSnippetHandler_ShouldEnrichFileWithSnippetData() throws Exception {
        packageHandler.processAllPackages(context, (p) -> {});

        snippetHandler.processAllSnippets(context);

        InventoryItem fileItem = context.getFileToInventoryItemMap().get("SPDXRef-File-1");
        Assertions.assertNotNull(fileItem, "InventoryItem for File-1 should exist");

        SoftwareComponent component = fileItem.getSoftwareComponent();

        boolean hasSnippetCopyright = component.getCopyrights().stream()
                .anyMatch(c -> c.getCopyrightText().equals("Copyright 2008-2010 John Smith"));
        Assertions.assertTrue(hasSnippetCopyright, "Component should contain snippet copyright");

        boolean hasSnippetLicense = component.getLicenses().stream()
                .anyMatch(l -> l.getLicenseName().equals("GPL-2.0-only"));
        Assertions.assertTrue(hasSnippetLicense, "Component should contain snippet license (GPL-2.0-only)");
    }


    @Test
    public void testRelationshipHandler_ShouldEstablishDependency() throws Exception {

        packageHandler.processAllPackages(context, (p) -> {});


        List<InventoryItem> allItems = inventoryItemRepository.findAllByProject(project);
        Map<String, InventoryItem> cache = new HashMap<>();
        for (InventoryItem item : allItems) {
            if (item.getSpdxId() != null) {
                cache.put(item.getSpdxId(), item);
            }
        }
        context.setInventoryCache(cache);

        relationshipHandler.processAllRelationships(context, (p) -> {});

        InventoryItem proj1 = context.getInventoryCache().get("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1");
        InventoryItem pkg1 = context.getInventoryCache().get("SPDXRef-Package-Maven-pkg1-grp-pkg1-0.0.1");

        Assertions.assertNotNull(proj1, "Project 1 should exist");
        Assertions.assertNotNull(pkg1, "Package 1 should exist");

        InventoryItem pkg1FromDb = inventoryItemRepository.findById(pkg1.getId()).orElseThrow();

        Assertions.assertNotNull(pkg1FromDb.getParent(), "Pkg1 should have a parent");
        Assertions.assertEquals(proj1.getId(), pkg1FromDb.getParent().getId(), "Proj1 should be the parent of Pkg1");
    }


    @Test
    public void testLicenseHandler_ShouldProcessExtractedAndListedLicenses() throws Exception {

        SpdxPackage pkg4 = getPackageById("SPDXRef-Package-Maven-pkg4-grp-pkg4-0.0.1");
        AnyLicenseInfo standardLicenseInfo = pkg4.getLicenseDeclared();
        SpdxPackage pkg6 = getPackageById("SPDXRef-Package-Maven-pkg6-grp-pkg6-0.0.1");
        AnyLicenseInfo extractedLicenseInfo = pkg6.getLicenseDeclared();

        List<License> standardResult = licenseHandler.createLicenses(
                standardLicenseInfo,
                context.getLicenseCache(),
                context.getExtractedLicenseInfos()
        );

        List<License> extractedResult = licenseHandler.createLicenses(
                extractedLicenseInfo,
                context.getLicenseCache(),
                context.getExtractedLicenseInfos()
        );


        Assertions.assertEquals(1, standardResult.size());
        License mit = standardResult.getFirst();
        Assertions.assertEquals("MIT", mit.getLicenseName());
        Assertions.assertTrue(mit.isSpdx(), "Standard license should be marked as SPDX");
        Assertions.assertFalse(licenseRepository.findByLicenseName("MIT").isEmpty(), "MIT should be persisted");

        Assertions.assertEquals(1, extractedResult.size());
        License asmus = extractedResult.getFirst();
        Assertions.assertEquals("LicenseRef-scancode-asmus", asmus.getLicenseName());
        Assertions.assertFalse(asmus.isSpdx(), "Extracted license should NOT be marked as standard SPDX");

        Assertions.assertTrue(asmus.getLicenseText().contains("ASMUS License"), "Should contain the extracted title");
        Assertions.assertTrue(asmus.getLicenseText().contains("This file contains bugs"), "Should contain the extracted body text");
        Assertions.assertFalse(licenseRepository.findByLicenseName("LicenseRef-scancode-asmus").isEmpty(), "Custom license should be persisted");
    }

    /**
     * Helper to safely find a package by its short SPDX ID (e.g., "SPDXRef-Pkg-1")
     * without needing to know the full Document Namespace URI.
     */
    private SpdxPackage getPackageById(String spdxId) throws Exception {
        return context.getSpdxDocument().getModelStore().getAllItems(null, "Package") // Get all Package URIs
                .map(typedValue -> {
                    try {
                        // Inflate URI to SpdxObject
                        return SpdxModelFactory.getSpdxObjects(
                                context.getSpdxDocument().getModelStore(),
                                context.getSpdxDocument().getCopyManager(),
                                "Package",
                                typedValue.getObjectUri(),
                                null
                        ).findFirst().orElse(null);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(obj -> obj instanceof SpdxPackage) // Ensure it is a Package
                .map(obj -> (SpdxPackage) obj)
                .filter(pkg -> {
                    try {
                        // Compare the Short ID
                        return pkg.getId().equals(spdxId);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Package not found in model: " + spdxId));
    }


}
