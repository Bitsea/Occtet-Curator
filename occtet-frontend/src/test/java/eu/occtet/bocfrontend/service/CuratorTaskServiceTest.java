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

import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.TaskStatus;
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
public class CuratorTaskServiceTest {

    @Autowired
    private CuratorTaskService curatorTaskService;
    @Autowired
    private DataManager dataManager;

    CuratorTask curatorTask;
    CuratorTask curatorTask1;
    @BeforeEach
    void setUp() {
        this.curatorTask = dataManager.create(CuratorTask.class);
        this.curatorTask1 = dataManager.create(CuratorTask.class);
        InventoryItem inventoryItem = dataManager.create(InventoryItem.class);
        Project project = dataManager.create(Project.class);

        this.curatorTask.setTaskName("TestImport1");
        this.curatorTask1.setTaskName("TestImport2");
        inventoryItem.setProject(project);

        dataManager.save(project);
        dataManager.save(inventoryItem);
    }

    @Test
    @Transactional
    void test_getTasksByStatus(){
        curatorTask1.setStatus(TaskStatus.IN_PROGRESS);
        dataManager.save(curatorTask1);
        assertEquals(1, curatorTaskService.getTasksByStatus(TaskStatus.IN_PROGRESS).size());
    }

    @Test
    @Transactional
    void test_getTasksByStatus_emptyOrNull(){
        assertEquals(0, curatorTaskService.getTasksByStatus(TaskStatus.IN_PROGRESS).size());
    }

    @Test
    @Transactional
    void test_countTasksByStatus(){
        assertEquals(0, curatorTaskService.countTasksByStatus(TaskStatus.IN_PROGRESS));
        curatorTask1.setStatus(TaskStatus.IN_PROGRESS);
        dataManager.save(curatorTask1);
        assertEquals(1, curatorTaskService.countTasksByStatus(TaskStatus.IN_PROGRESS));
        curatorTask.setStatus(TaskStatus.IN_PROGRESS);
        dataManager.save(curatorTask);
        assertEquals(2, curatorTaskService.countTasksByStatus(TaskStatus.IN_PROGRESS));
    }

    @Test
    @Transactional
    void test_updateTaskFeedback(){
        dataManager.save(curatorTask);
        curatorTaskService.updateTaskFeedback("test feedback", curatorTask);
        assertEquals(1, curatorTask.getFeedback().size());
        assertEquals("test feedback", curatorTask.getFeedback().get(0));
    }

    @Test
    @Transactional
    void test_updateTaskStatus() {
        dataManager.save(curatorTask);
        curatorTaskService.updateTaskStatus(curatorTask, TaskStatus.IN_PROGRESS);
        assertEquals(TaskStatus.IN_PROGRESS.getId(), curatorTask.getStatus());
        curatorTaskService.updateTaskStatus(curatorTask, TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED.getId(), curatorTask.getStatus());
    }
}
