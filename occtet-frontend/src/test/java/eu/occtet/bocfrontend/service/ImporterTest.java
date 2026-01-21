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

import eu.occtet.bocfrontend.entity.ImportTask;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.ImportStatus;
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
public class ImporterTest {

    @Autowired
    private ImportTaskService importTaskService;
    @Autowired
    private DataManager dataManager;

    ImportTask importTask;
    ImportTask importTask1;
    @BeforeEach
    void setUp() {
        this.importTask = dataManager.create(ImportTask.class);
        this.importTask1 = dataManager.create(ImportTask.class);
        InventoryItem inventoryItem = dataManager.create(InventoryItem.class);
        Project project = dataManager.create(Project.class);

        this.importTask.setImportName("TestImport1");
        this.importTask1.setImportName("TestImport2");
        inventoryItem.setProject(project);

        dataManager.save(project);
        dataManager.save(inventoryItem);
    }

    @Test
    @Transactional
    void test_getImportByStatus(){
        importTask1.setStatus(ImportStatus.IN_PROGRESS.getId());
        dataManager.save(importTask1);
        assertEquals(1, importTaskService.getImportByStatus(ImportStatus.IN_PROGRESS).size());
    }

    @Test
    @Transactional
    void test_getImportByStatus_emptyOrNull(){
        assertEquals(0, importTaskService.getImportByStatus(ImportStatus.IN_PROGRESS).size());
    }

    @Test
    @Transactional
    void test_countImportByStatus(){
        assertEquals(0, importTaskService.countImportByStatus(ImportStatus.IN_PROGRESS));
        importTask1.setStatus(ImportStatus.IN_PROGRESS.getId());
        dataManager.save(importTask1);
        assertEquals(1, importTaskService.countImportByStatus(ImportStatus.IN_PROGRESS));
        importTask.setStatus(ImportStatus.IN_PROGRESS.getId());
        dataManager.save(importTask);
        assertEquals(2, importTaskService.countImportByStatus(ImportStatus.IN_PROGRESS));
    }

    @Test
    @Transactional
    void test_updateImportFeedback(){
        dataManager.save(importTask);
        importTaskService.updateImportFeedback("test feedback", importTask);
        assertEquals(1, importTask.getFeedback().size());
        assertEquals("test feedback", importTask.getFeedback().get(0));
    }

    @Test
    @Transactional
    void test_updateImportStatus() {
        dataManager.save(importTask);
        importTaskService.updateImportStatus(ImportStatus.IN_PROGRESS, importTask);
        assertEquals(ImportStatus.IN_PROGRESS.getId(), importTask.getStatus());
        importTaskService.updateImportStatus(ImportStatus.COMPLETED, importTask);
        assertEquals(ImportStatus.COMPLETED.getId(), importTask.getStatus());
    }
}
