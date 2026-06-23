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

import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.CreationInfoEntity;
import eu.occtet.boc.entity.spdxV2.ExtractedLicensingInfoEntity;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.export.service.AnswerService;
import eu.occtet.boc.export.service.ExportService;
import eu.occtet.boc.export.service.handler.ComponentHandler;
import eu.occtet.boc.export.service.handler.VexDataHandler;
import eu.occtet.boc.model.SpdxExportWorkData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.parsers.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Aktiviert Mockito (kein Spring nötig!)
public class ExportServiceTest {


    @InjectMocks
    private ComponentHandler componentHandler;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Spy
    private VexDataHandler vexDataHandler;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private VexDataRepository vexDataRepository;

    @Mock
    private SoftwareComponentLicenseUsageRepository softwareComponentLicenseUsageRepository;

    private static final Logger log = LogManager.getLogger(ExportServiceTest.class);


    @Test
    void handleComponents_ShouldProcessVulnerabilitiesEvenWithoutVexData() throws GeneratorException {
        // Arrange
        Project project = new Project();
        project.setProjectName("Test Project");
        project.setVersion("1.0");

        SoftwareComponent softComp = new SoftwareComponent();
        softComp.setId(42L);
        softComp.setName("Log4j");
        softComp.setVersion("2.14.1");

        InventoryItem item = new InventoryItem();
        item.setId(100L);
        item.setSoftwareComponent(softComp);
        item.setInventoryName("log4j-item-ref");

        // connecting to vulnerability
        ComponentVulnerabilityLink link = new ComponentVulnerabilityLink();
        Vulnerability vuln = new Vulnerability();
        vuln.setVulnerabilityId("CVE-2021-44228");
        link.setVulnerability(vuln);
        link.setSoftwareComponent(softComp);

        softComp.addVulnerabilityLink(link);

        SoftwareComponent softComp2 = new SoftwareComponent();
        softComp2.setId(43L);
        softComp2.setName("Spring-Core");
        softComp2.setVersion("6.1.0");

        InventoryItem item2 = new InventoryItem();
        item2.setId(101L);
        item2.setSoftwareComponent(softComp2);
        item2.setInventoryName("spring-core-item-ref");

        ComponentVulnerabilityLink link2 = new ComponentVulnerabilityLink();
        Vulnerability vuln2 = new Vulnerability();
        vuln2.setVulnerabilityId("CVE-2024-22233");
        link2.setVulnerability(vuln2);
        link2.setSoftwareComponent(softComp2);

        softComp2.addVulnerabilityLink(link2);

        // configure mocks
        when(inventoryItemRepository.findAllByProject(project)).thenReturn(List.of(item, item2));        when(fileRepository.findAllByProject(project)).thenReturn(Collections.emptyList());
        when(softwareComponentLicenseUsageRepository.findUsageByProject(project)).thenReturn(Collections.emptyList());
        when(vexDataRepository.findBySoftwareComponentIds(anyList())).thenReturn(Collections.emptyList());


        // Act
        Bom incomingBom = new Bom();
        Metadata metadata= new Metadata();
        incomingBom.setMetadata(metadata);
        Bom resultBom = componentHandler.handleComponents(project,null, incomingBom, true);

        // Assert
        assertNotNull(resultBom);

        BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_16, resultBom);
        // generate json
        String jsonString = generator.toJsonString();

        log.debug("show sbom: {}", jsonString);

        // first component check
        assertNotNull(resultBom.getComponents());
        assertEquals(2, resultBom.getComponents().size(), "there should be two components");

        Component cdComponent1 = resultBom.getComponents().getFirst();
        assertEquals("Log4j", cdComponent1.getName());
        assertEquals("2.14.1", cdComponent1.getVersion());

        Component cdComponent2 = resultBom.getComponents().get(1);
        assertEquals("Spring-Core", cdComponent2.getName());
        assertEquals("6.1.0", cdComponent2.getVersion());

        // 2. vulnerability check
        assertNotNull(resultBom.getVulnerabilities());
        assertEquals(2, resultBom.getVulnerabilities().size(), "Es sollten zwei Schwachstellen in der BOM sein");
        assertEquals("CVE-2021-44228", resultBom.getVulnerabilities().get(0).getId());
        assertEquals("CVE-2024-22233", resultBom.getVulnerabilities().get(1).getId());

        org.cyclonedx.model.vulnerability.Vulnerability log4jVuln = resultBom.getVulnerabilities().stream()
                .filter(v -> "CVE-2021-44228".equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Log4j Schwachstelle nicht im BOM gefunden"));

        assertNotNull(log4jVuln.getAffects(), "affect should exist");
        assertFalse(log4jVuln.getAffects().isEmpty());
        assertEquals("log4j-item-ref", log4jVuln.getAffects().getFirst().getRef());

        // Verify
        verify(vexDataHandler, times(1)).mapToCycloneDxVulnerability(eq(vuln), isNull(), anyList());
        verify(vexDataHandler, times(1)).mapToCycloneDxVulnerability(eq(vuln2), isNull(), anyList());
    }




    @Test
    void testHandleComponents_ShouldExportDependencyGraphCorrectly() {
        // 1. GIVEN: Create a project and mock the environment
        Project project = new Project();
        project.setProjectName("TestProject");
        project.setVersion("1.0.0");

        // Initialize empty baseline responses for auxiliary repositories
        when(fileRepository.findAllByProject(project)).thenReturn(Collections.emptyList());
        when(softwareComponentLicenseUsageRepository.findUsageByProject(project)).thenReturn(Collections.emptyList());
        when(vexDataRepository.findBySoftwareComponentIds(anyList())).thenReturn(Collections.emptyList());

        // Create core SoftwareComponents with IDs (required for internal mapping/grouping keys)
        SoftwareComponent scMain = new SoftwareComponent(); scMain.setId(1L); scMain.setName("my-app");
        SoftwareComponent scSpring = new SoftwareComponent(); scSpring.setId(2L); scSpring.setName("spring-core");
        SoftwareComponent scLang = new SoftwareComponent(); scLang.setId(3L); scLang.setName("commons-lang3");

        // Create InventoryItems with proper inventory names (which act as bom-ref tokens)
        InventoryItem mainItem = new InventoryItem();
        mainItem.setId(10L);
        mainItem.setInventoryName("my-app 1.0.0");
        mainItem.setSoftwareComponent(scMain);
        mainItem.setParent(null); // Explicit root item

        InventoryItem springItem = new InventoryItem();
        springItem.setId(20L);
        springItem.setInventoryName("spring-core 6.0.0");
        springItem.setSoftwareComponent(scSpring);
        springItem.setParent(mainItem); // Tree hierarchy structure

        InventoryItem langItem = new InventoryItem();
        langItem.setId(30L);
        langItem.setInventoryName("commons-lang3 3.12.0");
        langItem.setSoftwareComponent(scLang);
        langItem.setParent(springItem); // Tree hierarchy structure

        // Setup the outgoing dependency relationships (The Graph Logics)
        mainItem.addDependencies(Set.of(springItem));
        springItem.addDependencies(Set.of(langItem));
        langItem.addDependencies(Collections.emptySet());

        // Mock inventory repository to return all three simulated database rows
        List<InventoryItem> dbInventoryItems = List.of(mainItem, springItem, langItem);
        when(inventoryItemRepository.findAllByProject(project)).thenReturn(dbInventoryItems);

        // Initialize target CycloneDX BOM containing empty metadata context
        Bom incomingBom = new Bom();
        incomingBom.setMetadata(new Metadata());

        // 2. WHEN: Execute the handler method to transform entities into BOM fields
        Bom resultBom = componentHandler.handleComponents(project, null, incomingBom, false);

        // 3. THEN: Verify that the dependency matrix was compiled seamlessly
        assertNotNull(resultBom, "The resulting BOM should not be null");
        List<Dependency> bomDependencies = resultBom.getDependencies();
        assertNotNull(bomDependencies, "BOM dependencies section should have been created");

        // Since only 'my-app' and 'spring-core' declare dependencies, we expect exactly 2 root elements
        assertEquals(2, bomDependencies.size(), "Should contain exactly 2 dependency nodes with children");

        // Validate Node 1: my-app -> spring-core
        Dependency mainNode = bomDependencies.stream()
                .filter(d -> "my-app 1.0.0".equals(d.getRef()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dependency root node for 'my-app 1.0.0' missing"));

        assertNotNull(mainNode.getDependencies(), "Sub-dependencies of main app should not be null");
        assertEquals(1, mainNode.getDependencies().size());
        assertEquals("spring-core 6.0.0", mainNode.getDependencies().getFirst().getRef(),
                "Main app should point to spring-core as child dependency");

        // Validate Node 2: spring-core -> commons-lang3
        Dependency springNode = bomDependencies.stream()
                .filter(d -> "spring-core 6.0.0".equals(d.getRef()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dependency root node for 'spring-core 6.0.0' missing"));

        assertNotNull(springNode.getDependencies(), "Sub-dependencies of spring-core should not be null");
        assertEquals(1, springNode.getDependencies().size());
        assertEquals("commons-lang3 3.12.0", springNode.getDependencies().getFirst().getRef(),
                "Spring-core should point to commons-lang3 as child dependency");
    }

}
