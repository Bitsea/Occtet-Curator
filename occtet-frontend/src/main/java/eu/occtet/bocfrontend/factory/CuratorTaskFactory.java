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

package eu.occtet.bocfrontend.factory;


import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class CuratorTaskFactory {

    @Autowired
    private DataManager dataManager;

    /**
     * create ImportTask entity for given project
     * @param project the origin project
     * @param name of the importer to use for scanning this softwareComponent.
     * @return the persisted Importer entity
     */
    public CuratorTask create(@Nonnull Project project, @Nonnull String name, @Nonnull String type) {
        CuratorTask curatorTask = dataManager.create(CuratorTask.class);
        curatorTask.setProject(project);
        curatorTask.setTaskName(name);
        curatorTask.setTaskType(type);

        return dataManager.save(curatorTask);
    }


}
