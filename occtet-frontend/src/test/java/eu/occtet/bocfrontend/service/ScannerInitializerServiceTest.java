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

package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class ScannerInitializerServiceTest {

    @Autowired
    private ScannerInitializerService scannerInitializerService;
    @Autowired
    private DataManager dataManager;

    ScannerInitializer scannerInitializer1;
    ScannerInitializer scannerInitializer2;
    @BeforeEach
    void setUp() {
        this.scannerInitializer1 = dataManager.create(ScannerInitializer.class);
        this.scannerInitializer2 = dataManager.create(ScannerInitializer.class);
        InventoryItem inventoryItem = dataManager.create(InventoryItem.class);
        Project project = dataManager.create(Project.class);

        this.scannerInitializer1.setScanner("TestScanner1");
        this.scannerInitializer2.setScanner("TestScanner2");
        this.scannerInitializer1.setInventoryItem(inventoryItem);
        this.scannerInitializer2.setInventoryItem(inventoryItem);
        inventoryItem.setProject(project);

        dataManager.save(project);
        dataManager.save(inventoryItem);
    }

    @Test
    @Transactional
    void test_getScannerByStatus(){
        scannerInitializer2.setStatus(ScannerInitializerStatus.IN_PROGRESS.getId());
        dataManager.save(scannerInitializer2);
        assertEquals(1, scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.IN_PROGRESS).size());
    }

    @Test
    @Transactional
    void test_getScannerByStatus_emptyOrNull(){
        assertEquals(0, scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.IN_PROGRESS).size());
    }

    @Test
    @Transactional
    void test_countScannerByStatus(){
        assertEquals(0, scannerInitializerService.countScannerByStatus(ScannerInitializerStatus.IN_PROGRESS));
        scannerInitializer2.setStatus(ScannerInitializerStatus.IN_PROGRESS.getId());
        dataManager.save(scannerInitializer2);
        assertEquals(1, scannerInitializerService.countScannerByStatus(ScannerInitializerStatus.IN_PROGRESS));
        scannerInitializer1.setStatus(ScannerInitializerStatus.IN_PROGRESS.getId());
        dataManager.save(scannerInitializer1);
        assertEquals(2, scannerInitializerService.countScannerByStatus(ScannerInitializerStatus.IN_PROGRESS));
    }

    @Test
    @Transactional
    void test_updateScannerFeedback(){
        dataManager.save(scannerInitializer1);
        scannerInitializerService.updateScannerFeedback("test feedback", scannerInitializer1);
        assertEquals(1, scannerInitializer1.getFeedback().size());
        assertEquals("test feedback", scannerInitializer1.getFeedback().get(0));
    }

    @Test
    @Transactional
    void test_updateScannerStatus() {
        dataManager.save(scannerInitializer1);
        scannerInitializerService.updateScannerStatus(ScannerInitializerStatus.IN_PROGRESS, scannerInitializer1);
        assertEquals(ScannerInitializerStatus.IN_PROGRESS.getId(), scannerInitializer1.getStatus());
        scannerInitializerService.updateScannerStatus(ScannerInitializerStatus.COMPLETED, scannerInitializer1);
        assertEquals(ScannerInitializerStatus.COMPLETED.getId(), scannerInitializer1.getStatus());
    }
}
