/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.factory.InventoryItemFactory;
import eu.occtet.bocfrontend.factory.LicenseFactory;
import eu.occtet.bocfrontend.factory.ProjectFactory;
import eu.occtet.bocfrontend.factory.SoftwareComponentFactory;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@Transactional
public class SoftwareVexComponentServiceTest {

    private static final Logger log = LogManager.getLogger(SoftwareVexComponentServiceTest.class);

    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private SoftwareComponentFactory softwareComponentFactory;
    @Autowired
    private InventoryItemFactory inventoryItemFactory;
    @Autowired
    private ProjectFactory projectFactory;
    @Autowired
    private LicenseFactory licenseFactory;

    Project project;
    License license;
    InventoryItem item1;
    InventoryItem item2;
    SoftwareComponent softwareComponent1;
    SoftwareComponent softwareComponent2;
    SoftwareComponent softwareComponent3;

    @BeforeEach
    void setUp() {
        project = projectFactory.create("SoftwareComponentServiceTestProject");
        license = licenseFactory.create("SoftwareComponentServiceTestLicenseType",
                "SoftwareComponentServiceTestLicenseText","SoftwareComponentServiceTestLicenseName");
        List<License> licenses = new ArrayList<>(List.of(license));
        softwareComponent1 = softwareComponentFactory.create("SoftwareComponentServiceTestSc1", "1.0",
                "","CVE-2003-2034","","",true,licenses);
        softwareComponent2 = softwareComponentFactory.create("SoftwareComponentServiceTestSc2", "2.0",
                "","CVE-2005-1234","","",false,licenses);
        softwareComponent3 = softwareComponentFactory.create("SoftwareComponentServiceTestSc3", "3.0",
                "","","","",false,new ArrayList<>());
        item1 = inventoryItemFactory.create("SoftwareComponentServiceTestItem1", softwareComponent1, project);
        item2 = inventoryItemFactory.create("SoftwareComponentServiceTestItem2", softwareComponent2, project);
    }


    @Test
    void testFindSoftwareComponentsByProject(){
        assertEquals(2, softwareComponentService.findSoftwareComponentsByProject(project).size());
    }

    @Test
    void testFindSoftwareComponentsByLicense(){
        assertEquals(2, softwareComponentService.findSoftwareComponentsByLicense(license).size());
    }

    @Test
    void testGetCVEDescriptionsList(){
        String example = "CVE-2008-0986_x000D_ CVE-2008-0985 ";
        Set<String> result = softwareComponentService.getCVEDescriptionsList(example);
        log.debug("result: "+result);
        assertEquals(2, result.size());
        assertTrue(result.contains("CVE-2008-0986_x000D_"));
        assertTrue(result.contains("CVE-2008-0985"));
    }

    @Test
    void testFindComponentsByIsVulnerable(){
    }
}
