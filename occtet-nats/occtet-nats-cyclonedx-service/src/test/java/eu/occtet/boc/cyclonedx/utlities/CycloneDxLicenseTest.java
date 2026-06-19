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
import eu.occtet.boc.cyclonedx.service.handler.*;
import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.model.vexModels.VexVulnerability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
import java.util.List;
import java.util.Set;


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
public class CycloneDxLicenseTest {

    private static final Logger log = LogManager.getLogger(CycloneDxLicenseTest.class);

    private static final String TEST_FILE_NAME = "synthetic-cyclonedx-license.json";

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
    public void testComponentHandler() {

        componentHandler.processAllPackages(context, null , bom, true);


        List<SoftwareComponent> components = softwareComponentRepository.findComponentsByProject(project);
        log.debug("Components size: {}", components.size());
        Set<InventoryItem> inventoryItems = context.getInventoryItems();
        List<License> licenses= context.getLicenseCache().values().stream().toList();
        log.debug("Licenses size: {}", licenses.size());


        Assertions.assertFalse(licenses.isEmpty(), "Licenses for project should exist");

        Assertions.assertNotNull(inventoryItems, "IventoryItems should not be null");
        Assertions.assertNotNull(licenses, "license cache should not be null");

        Assertions.assertEquals(2, licenses.size());
        Assertions.assertTrue(licenses.stream().anyMatch(license -> license.getLicenseType().equals("Apache-2.0")));
        Assertions.assertTrue(licenses.stream().anyMatch(license -> license.getLicenseType().equals("MIT")));

        Assertions.assertEquals(2, context.getUsageLicenseCache().size());

        Assertions.assertEquals(4, components.size());
        Assertions.assertTrue(components.stream().anyMatch(component -> component.getName().equals("slf4j-api") &&
                component.getVersion().equals("2.0.7")));
        Assertions.assertTrue(components.stream().anyMatch(component -> component.getName().equals("my-microservice") &&
                component.getVersion().equals("1.0.0")));
        Assertions.assertTrue(components.stream().anyMatch(component -> component.getName().equals("StringUtils.class") &&
                component.getVersion().equals("unknown")));
        Assertions.assertTrue(components.stream().anyMatch(component -> component.getName().equals("commons-lang3") &&
                component.getVersion().equals("3.12.0")));

        Assertions.assertEquals(4, inventoryItems.size());
        Assertions.assertTrue(inventoryItems.stream().anyMatch(item -> item.getInventoryName().contains("commons-lang3") &&
                item.getInventoryName().contains("3.12.0") && item.getInventoryName().contains("Apache-2.0")));
        Assertions.assertTrue(inventoryItems.stream().anyMatch(item -> item.getInventoryName().contains("my-microservice") &&
                item.getInventoryName().contains("1.0.0")));
        Assertions.assertTrue(inventoryItems.stream().anyMatch(item -> item.getInventoryName().contains("StringUtils.class") &&
                item.getInventoryName().contains("unknown")));
        Assertions.assertTrue(inventoryItems.stream().anyMatch(item -> item.getInventoryName().contains("slf4j-api") &&
                item.getInventoryName().contains("2.0.7") && item.getInventoryName().contains("MIT")));

    }

}
