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

package eu.occtet.bocfrontend.security.oidc;

import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

@RowLevelRole(name = "Curator Data Access", code = "curator")
public interface CuratorRowLevelRole {

    @JpqlRowLevelPolicy(
            entityClass = Project.class,
            where = "{E}.createdBy = :current_user_username"
    )
    void projectCreatorOnly();

    @JpqlRowLevelPolicy(
            entityClass = CuratorTask.class,
            where = "{E}.createdBy = :current_user_username"
    )
    void assignedTasksOnly();

    @JpqlRowLevelPolicy(
            entityClass = InventoryItem.class,
            where = "{E}.project.createdBy = :current_user_username"
    )
    void inventoryForMyProjectsOnly();

    // TODO the same to other entities such as SoftwareComponent which as discusses should only show for the creator
    //  for now <- this might require some changes to the entities themselves
    // FIXME This can be done using the createdBy field, meaning when the user sends a message to the
    // nats-micorservice we need to send the logged in username within the WorkTask messages and later when entities
    // gets created we need to set the user manually
}
