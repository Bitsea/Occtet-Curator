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

package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.Organization;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProjectRepository extends JmixDataRepository<Project, Long> {
    List<Project> findAll();
    @Query("select p from project p where p.projectName = :name and p.version = :version")
    List<Project> findByNameAndVersion(String name, String version);
    List<Project> findByProjectName(String name);
    @Query("select p from Project p where p.organization IS null")
    List<Project> findAvailableProjects();
    List<Project> findByOrganization(Organization organization);

    @Query("select count(p) from Project p where p.projectName = :name and p.version = :version and p.organization = :organization and p.id <> :ignoreId")
    long countDuplicates(@Param("name") String name,
                         @Param("version") String version,
                         @Param("organization") Organization organization,
                         @Param("ignoreId") Long ignoreId);
}
