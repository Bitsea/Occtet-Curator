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

package eu.occtet.bocfrontend.engine;

import eu.occtet.bocfrontend.dao.CuratorTaskRepository;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.factory.InventoryItemFactory;
import eu.occtet.bocfrontend.factory.ProjectFactory;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.factory.SoftwareComponentFactory;
import eu.occtet.bocfrontend.importer.TaskManager;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.core.security.Authenticated;
import io.jmix.core.security.SystemAuthenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureDataJpa
public class TaskManagerTest {

    @Autowired
    private TaskManager taskManager;


    @Autowired
    private DataManager dataManager;

    @Autowired
    private FetchPlans fetchPlans;

    @Autowired
    private CuratorTaskRepository curatorTaskRepository;

    @Autowired
    private CuratorTaskFactory curatorTaskFactory;

    @Autowired
    private SoftwareComponentFactory softwareComponentFactory;

    @Autowired
    private InventoryItemFactory inventoryItemFactoryFactory;

    @Autowired
    private ProjectFactory projectFactory;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    private static final Logger log = LogManager.getLogger(TaskManagerTest.class);


    @Authenticated
    private Project prepare() {

        Project project = projectFactory.create("project1");
        dataManager.save(project);
        return project;
    }


    @Test
    public void testWithDumbImport() {

        systemAuthenticator.runWithSystem(()-> {

            CuratorTask curatorTask = curatorTaskFactory.create(prepare(), "dumb","import.test");
            log.debug("Created task: {}", curatorTask);
            //TODO write nice test here
        });
    }

}
