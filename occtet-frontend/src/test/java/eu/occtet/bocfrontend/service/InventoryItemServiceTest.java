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
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.factory.InventoryItemFactory;
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

@SpringBootTest
@Transactional
@ExtendWith(AuthenticatedAsAdmin.class)
public class InventoryItemServiceTest {

    private static final Logger log = LogManager.getLogger(InventoryItemServiceTest.class);


    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private InventoryItemFactory inventoryItemFactory;
    @Autowired
    private SoftwareComponentFactory softwareComponentFactory;
    @Autowired
    private ProjectFactory projectFactory;

    InventoryItem item1;
    InventoryItem item2;
    Project project1;
    SoftwareComponent softwareComponent1;

    @BeforeEach
    void setUp() {
        project1 = projectFactory.create("InventoryItemServiceTestProject");
        softwareComponent1 = softwareComponentFactory.create("InventoryItemServiceTestSc1", "1.0",
                "purl-123",true,null);
        item1 = inventoryItemFactory.create("InventoryItemServiceTestItem1", softwareComponent1, project1);
        item2 = inventoryItemFactory.create("InventoryItemServiceTestItem2", softwareComponent1, project1);
    }

    @Test
    void testFindInventoryItemBySoftwareComponentCve(){
    }

    @Test
    void testFindInventoryItemsByIsVulnerable(){
    }
}
