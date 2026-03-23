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

import eu.occtet.bocfrontend.entity.*;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

@RowLevelRole(
        name = "Curator Data Access",
        code = CuratorRole.CODE)
public interface CuratorRowLevelRole {

    // DIRECTLY CONNECTED ENTITIES (Implementing HasOrganization)

    @JpqlRowLevelPolicy(entityClass = Project.class, where = "{E}.organization = :current_user_organization")
    void project();

    @JpqlRowLevelPolicy(entityClass = SoftwareComponent.class, where = "{E}.organization = :current_user_organization")
    void softwareComponent();

    @JpqlRowLevelPolicy(entityClass = Vulnerability.class, where = "{E}.organization = :current_user_organization")
    void vulnerability();

    @JpqlRowLevelPolicy(entityClass = InventoryItem.class, where = "{E}.organization = :current_user_organization")
    void inventoryItem();

    @JpqlRowLevelPolicy(entityClass = License.class, where = "{E}.organization = :current_user_organization")
    void license();

    @JpqlRowLevelPolicy(entityClass = Copyright.class, where = "{E}.organization = :current_user_organization")
    void copyright();

    @JpqlRowLevelPolicy(entityClass = Suggestion.class, where = "{E}.organization = :current_user_organization")
    void suggestion();

    // INDIRECTLY CONNECTED ENTITIES (Via Parent Hierarchy)

    @JpqlRowLevelPolicy(entityClass = File.class, where = "{E}.project.organization = :current_user_organization")
    void file();

    @JpqlRowLevelPolicy(
            entityClass = VexData.class,
            where = "{E}.softwareComponent.organization = :current_user_organization"
    )
    void vexData();

    @JpqlRowLevelPolicy(entityClass = OrtIssue.class, where = "{E}.project.organization = :current_user_organization")
    void ortIssue();

    @JpqlRowLevelPolicy(entityClass = OrtViolation.class, where = "{E}.project.organization = :current_user_organization")
    void ortViolation();

    @JpqlRowLevelPolicy(entityClass = CuratorTask.class, where = "{E}.project.organization = :current_user_organization")
    void curatorTask();

    @JpqlRowLevelPolicy(entityClass = ComponentVulnerabilityLink.class, where = "{E}.softwareComponent.organization = :current_user_organization")
    void componentVulnerabilityLink();

}




