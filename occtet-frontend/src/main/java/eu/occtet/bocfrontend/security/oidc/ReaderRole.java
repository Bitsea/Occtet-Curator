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

import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name="Reader", code = ReaderRole.CODE)
public interface ReaderRole {

    String CODE = "reader";

    @MenuPolicy(menuIds = {
            "DashboardView", "AuditView", "VexData.list", "Vulnerability.list",
            "InventoryItem.list", "Copyright.list", "License.list", "Project.list",
            "SoftwareComponent.list", "ortIssue.list", "ortViolation.list",
            "CuratorTask.list", "HelpView"
    })
    void menuAccess();

    @ViewPolicy(viewIds = {
            "DashboardView", "AuditView", "HelpView",
            "VexData.list", "VexData.detail",
            "Vulnerability.list", "Vulnerability.detail",
            "InventoryItem.list", "InventoryItem.detail",
            "Copyright.list", "Copyright.detail",
            "License.list", "License.detail",
            "Project.list", "Project.detail",
            "SoftwareComponent.list", "SoftwareComponent.detail",
            "ortIssue.list", "ortIssue.detail",
            "ortViolation.list", "ortViolation.detail",
            "CuratorTask.list", "CuratorTask.detail"
    })
    void viewAccess();

    @EntityPolicy(entityName = "*", actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityName = "*", attributes = "*", action = EntityAttributePolicyAction.VIEW)
    void readOnlyEntityAccess();
}