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

package eu.occtet.boc.cyclonedx.utlities;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.config.TestEclipseLinkJpaConfiguration;
import eu.occtet.boc.cyclonedx.context.CycloneDxImportContext;
import eu.occtet.boc.cyclonedx.factory.*;
import eu.occtet.boc.cyclonedx.service.*;
import eu.occtet.boc.cyclonedx.service.handler.ComponentHandler;
import eu.occtet.boc.cyclonedx.service.handler.LicenseHandler;
import eu.occtet.boc.cyclonedx.service.handler.VulnerabilityHandler;
import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.Bom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        CycloneDxService.class, SoftwareComponentService.class, SoftwareComponentRepository.class,
        CopyrightService.class, InventoryItemService.class, LicenseService.class, FileService.class,
        ProjectRepository.class, InventoryItemRepository.class, SoftwareComponentFactory.class, FileRepository.class,
        CopyrightFactory.class, FileFactory.class, InventoryItemFactory.class, CleanUpService.class, TestEclipseLinkJpaConfiguration.class,
        LicenseHandler.class, ComponentHandler.class,  SoftwareComponentLicenseUsageService.class,
        LicenseRepository.class, License.class, SoftwareComponentLicenseUsageService.class, SoftwareComponentLicenseUsageFactory.class,
        SoftwareComponentLicenseUsageRepository.class, VulnerabilityHandler.class,
        VulnerabilityService.class, VulnerabilityFactory.class, ComponentVulnerabilityLinkService.class,
        ComponentVulnerabilityLinkFactory.class, ComponentVulnerabilityLinkRepository.class, VexDataFactory.class, VexDataRepository.class
})
@EnableJpaRepositories(basePackages = {
        "eu.occtet.boc.dao"})
@EntityScan(basePackages = {
        "eu.occtet.boc.entity"
})
@ExtendWith(MockitoExtension.class)
public class CycloneDxDependencyTest{

    private static final Logger log = LogManager.getLogger(CycloneDxLicenseTest.class);

    private static final String TEST_FILE_NAME = "test-dependencies-cdx.json";

    @MockitoBean
    private AnswerService answerService;
    @MockitoBean
    private CleanUpService cleanUpService;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private LicenseHandler licenseHandler;
    @Autowired
    private ComponentHandler componentHandler;

    private Project project;
    private CycloneDxImportContext context;
    private Organization organization;
    private ObjectMapper objectMapper;
    private Bom bom;

    @BeforeEach
    public void setup() throws Exception {
        objectMapper= new ObjectMapper();

        organization = new Organization();
        organization.setOrganizationName("TestOrganization");
        organization= organizationRepository.saveAndFlush(organization);

        project = new Project();
        project.setProjectName("IntegrationTestProject");
        project.setVersion("1.0.0");
        project.setOrganization(organization);

        project = projectRepository.saveAndFlush(project);

        context = new CycloneDxImportContext(project);


        InputStream inputStream = new ClassPathResource(TEST_FILE_NAME).getInputStream();
        bom = objectMapper.readValue(inputStream, Bom.class);
    }

    @Test
    void testProcessAllPackages_ShouldResolveDependencyGraphCorrectly() throws Exception {
        componentHandler.processAllPackages(context, null , bom, true);


        List<SoftwareComponent> components = softwareComponentRepository.findComponentsByProject(project);
        log.debug("Components size: {}", components.size());
        Set<InventoryItem> savedItems = context.getInventoryItems();

        // Since the main component (my-app) from the metadata typically ends up in context.getMainInventoryItems()
        // and the other ones are located in bom.getComponents(), we check the extracted components here:
        assertEquals(3, savedItems.size(), "Exactly 3 libraries should be in inventoryItemsToSave");

        InventoryItem springCore = savedItems.stream()
                .filter(item -> item.getInventoryName().contains("spring-core"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("spring-core not found"));

        InventoryItem commonsLang = savedItems.stream()
                .filter(item -> item.getInventoryName().contains("commons-lang3"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("commons-lang3 not found"));

        InventoryItem mainApp = context.getMainInventoryItems().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Main component not found in context"));

        assertTrue(mainApp.getDependencies().size()==0, "Dependencies of mainApp must be 0");

        assertNotNull(springCore.getDependencies(), "Dependencies of spring-core are not allowed to be null");
        assertTrue(springCore.getDependencies().size()==1, "one dependency from spring-core");
        assertTrue(springCore.getDependencies().stream().anyMatch(i-> i.getInventoryName().equals(commonsLang.getInventoryName())), "spring-core must declare commons-lang3 as a dependency");

        assertTrue(commonsLang.getDependencies() == null || commonsLang.getDependencies().isEmpty(), "commons-lang3 should have no sub-dependencies");
    }
}