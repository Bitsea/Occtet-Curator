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
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.security.model.RowLevelBiPredicate;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;

@RowLevelRole(name = "Curator Data Access", code = "curator")
public interface CuratorRowLevelRole {


//    @JpqlRowLevelPolicy(
//            entityClass = Project.class,
//            where = "{E}.organization.id = :current_user_organization_id"
//    )
//    void projectPolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = Vulnerability.class,
//            where = "{E}.organization.id = :current_user_organization_id"
//    )
//    void vulnerabilityPolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = SoftwareComponent.class,
//            where = "{E}.organization.id = :current_user_organization_id"
//    )
//    void softwareComponentPolicy();
//
//
//    @JpqlRowLevelPolicy(
//            entityClass = InventoryItem.class,
//            where = "{E}.project.organization.id = :current_user_organization_id"
//    )
//    void inventoryItemPolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = OrtIssue.class,
//            where = "{E}.project.organization.id = :current_user_organization_id"
//    )
//    void ortIssuePolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = OrtViolation.class,
//            where = "{E}.project.organization.id = :current_user_organization_id"
//    )
//    void ortViolationPolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = File.class,
//            where = "{E}.project.organization.id = :current_user_organization_id"
//    )
//    void filePolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = Copyright.class,
//            where = "{E}.project.organization.id = :current_user_organization_id"
//    )
//    void copyrightPolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = CuratorTask.class,
//            where = "{E}.project.organization.id = :current_user_organization_id"
//    )
//    void curatorTaskPolicy();
//
//    @JpqlRowLevelPolicy(
//            entityClass = VexData.class,
//            where = "{E}.software_component.organization.id = :current_user_organization_id"
//    )
//    void vexDataPolicy();



        default boolean isAllowed(User user, Organization entityOrg) {

            // Normale User: nur gleiche Organisation
            return entityOrg != null && entityOrg.equals(user.getOrganization());
        }

        // ------------------- DIRECT ORGANIZATION -------------------
        @PredicateRowLevelPolicy(entityClass = Project.class, actions = RowLevelPolicyAction.UPDATE)
        default RowLevelBiPredicate<Project, ApplicationContext> projectPolicy() {
            return (entity, applicationContext) -> {
                CurrentAuthentication currentAuthentication =
                        applicationContext.getBean(CurrentAuthentication.class);

                User user = (User) currentAuthentication.getUser();

                return entity.getOrganization() != null
                        && isAllowed(user, entity.getOrganization());
            };
        }

//
//        @PredicateRowLevelPolicy(entityClass = Vulnerability.class, actions = RowLevelPolicyAction.READ)
//        default boolean vulnerabilityPolicy(Vulnerability entity, Authentication auth) {
//            return isAllowed((User) auth.getPrincipal(), entity.getOrganization());
//        }
//
//        @PredicateRowLevelPolicy(entityClass = SoftwareComponent.class, actions = RowLevelPolicyAction.READ)
//        default boolean softwareComponentPolicy(SoftwareComponent entity, Authentication auth) {
//            return isAllowed((User) auth.getPrincipal(), entity.getOrganization());
//        }
//
//        // ------------------- VIA PROJECT -------------------
//
//        @PredicateRowLevelPolicy(entityClass = InventoryItem.class, actions = RowLevelPolicyAction.READ)
//        default boolean inventoryItemPolicy(InventoryItem entity, Authentication auth) {
//            return entity.getProject() != null &&
//                    isAllowed((User) auth.getPrincipal(), entity.getProject().getOrganization());
//        }
//
//        @PredicateRowLevelPolicy(entityClass = OrtIssue.class, actions = RowLevelPolicyAction.READ)
//        default boolean ortIssuePolicy(OrtIssue entity, Authentication auth) {
//            return entity.getProject() != null &&
//                    isAllowed((User) auth.getPrincipal(), entity.getProject().getOrganization());
//        }
//
//        @PredicateRowLevelPolicy(entityClass = OrtViolation.class, actions = RowLevelPolicyAction.READ)
//        default boolean ortViolationPolicy(OrtViolation entity, Authentication auth) {
//            return entity.getProject() != null &&
//                    isAllowed((User) auth.getPrincipal(), entity.getProject().getOrganization());
//        }
//
//        @PredicateRowLevelPolicy(entityClass = File.class, actions = RowLevelPolicyAction.READ)
//        default boolean filePolicy(File entity, Authentication auth) {
//            return entity.getProject() != null &&
//                    isAllowed((User) auth.getPrincipal(), entity.getProject().getOrganization());
//        }

    @PredicateRowLevelPolicy(entityClass = CuratorTask.class, actions = RowLevelPolicyAction.UPDATE)
    default RowLevelBiPredicate<CuratorTask, ApplicationContext> curatorTaskPolicy() {
        return (entity, applicationContext) -> {
            CurrentAuthentication currentAuthentication =
                    applicationContext.getBean(CurrentAuthentication.class);

            User user = (User) currentAuthentication.getUser();

            return entity.getProject() != null
                    && isAllowed(user, entity.getProject().getOrganization());
        };
    }


        // ------------------- VIA SOFTWARE COMPONENT -------------------

    @PredicateRowLevelPolicy(entityClass = VexData.class, actions = RowLevelPolicyAction.UPDATE)
    default RowLevelBiPredicate<VexData, ApplicationContext> vexDataPolicy() {
        return (entity, applicationContext) -> {
            CurrentAuthentication currentAuthentication =
                    applicationContext.getBean(CurrentAuthentication.class);

            User user = (User) currentAuthentication.getUser();

            return entity.getSoftwareComponent() != null
                    && isAllowed(user, entity.getSoftwareComponent().getOrganization());
        };
    }
}




