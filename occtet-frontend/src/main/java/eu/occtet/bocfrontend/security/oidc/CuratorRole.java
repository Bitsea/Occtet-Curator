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

import eu.occtet.bocfrontend.entity.Project;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name="Curator", code = CuratorRole.CODE)
public interface CuratorRole {

    String CODE = "curator";

    @MenuPolicy(menuIds = {
            "DashboardView",
            "AuditView",
            "VexData.list",
            "Vulnerability.list",
            "InventoryItem.list",
            "Copyright.list",
            "License.list",
            "Project.list",
            "SoftwareComponent.list",
            "ortIssue.list",
            "ortViolation.list",
            "CuratorTask.list",
            "HelpView"
    })
    void menuNavigationAccess();

    @ViewPolicy(viewIds = {
            "DashboardView",
            "AuditView",
            "HelpView",
            "CuratorTask.list",
            "CuratorTask.detail",
            "ExportProjectSbomHelperView"
    })
    void coreCurationViewAccess();

    @ViewPolicy(viewIds = {
            "VexData.list", "VexData.detail",
            "Vulnerability.list", "Vulnerability.detail",
            "ortIssue.list", "OrtIssue.detail",
            "ortViolation.list", "OrtViolation.detail"
    })
    void securityDomainViewAccess();

    @ViewPolicy(viewIds = {
            "InventoryItem.list", "InventoryItem.detail",
            "Copyright.list", "Copyright.detail",
            "License.list", "License.detail",
            "Project.list", "Project.detail",
            "SoftwareComponent.list", "SoftwareComponent.detail",
            "File.list",
            "SearchTermsProfile.list", "SearchTermsProfileDetailView",
            "Suggestion.list", "Suggestion.detail"
    })
    void inventoryDomainViewAccess();

    @ViewPolicy(viewIds = {
            "Configuration.list",
            "Configuration.detail"
    })
    void configurationEntityViewAccess();

    @ViewPolicy(viewIds = {
            "addCopyrightDialog",
            "addLicenseDialog",
            "addLicenseToCopyrightDialog",
            "createCopyrightDialog",
            "createLicenseDialog",
            "overviewContentInfoDialog",
            "LicenseDialogWindowView"
    })
    void dialogAndHelperViewAccess();

    @EntityPolicy(entityClass = Project.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Project.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    void projectUpdateAccess();

    @EntityPolicy(entityName = "*", actions = {EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE, EntityPolicyAction.DELETE})
    @EntityAttributePolicy(entityName = "*", attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    void defaultEntityAccess();
}