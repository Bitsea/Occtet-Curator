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

@RowLevelRole(name = "Curator Data Access", code = "curator")
public interface CuratorRowLevelRole {

    @JpqlRowLevelPolicy(
            entityClass = Project.class,
            where = "{E}.organization.id = :current_user_organization_id"
    )
    void projectPolicy();

    @JpqlRowLevelPolicy(
            entityClass = Vulnerability.class,
            where = "{E}.organization.id = :current_user_organization_id"
    )
    void vulnerabilityPolicy();

    @JpqlRowLevelPolicy(
            entityClass = SoftwareComponent.class,
            where = "{E}.organization.id = :current_user_organization_id"
    )
    void softwareComponentPolicy();


    @JpqlRowLevelPolicy(
            entityClass = InventoryItem.class,
            where = "{E}.project.organization.id = :current_user_organization_id"
    )
    void inventoryItemPolicy();

    @JpqlRowLevelPolicy(
            entityClass = OrtIssue.class,
            where = "{E}.project.organization.id = :current_user_organization_id"
    )
    void ortIssuePolicy();

    @JpqlRowLevelPolicy(
            entityClass = OrtViolation.class,
            where = "{E}.project.organization.id = :current_user_organization_id"
    )
    void ortViolationPolicy();

    @JpqlRowLevelPolicy(
            entityClass = File.class,
            where = "{E}.project.organization.id = :current_user_organization_id"
    )
    void filePolicy();

    @JpqlRowLevelPolicy(
            entityClass = Copyright.class,
            where = "{E}.project.organization.id = :current_user_organization_id"
    )
    void copyrightPolicy();

    @JpqlRowLevelPolicy(
            entityClass = CuratorTask.class,
            where = "{E}.project.organization.id = :current_user_organization_id"
    )
    void curatorTaskPolicy();

    @JpqlRowLevelPolicy(
            entityClass = VexData.class,
            where = "{E}.software_component.organization.id = :current_user_organization_id"
    )
    void vexDataPolicy();





}
