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

package eu.occtet.bocfrontend.usermanagement;

import eu.occtet.bocfrontend.entity.Project;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

@RowLevelRole(name = "projectMember", code = "projectMember")
public interface ProjectMemberRole {

    @JpqlRowLevelPolicy(
            entityClass = Project.class,
            where = "{E}.id in (select pm.project.id from ProjectMember pm where pm.username = :current_user_username)"
    )
    void projectPolicy();

    //TODO also all other data is only supposed to be seen for projectUser
}
