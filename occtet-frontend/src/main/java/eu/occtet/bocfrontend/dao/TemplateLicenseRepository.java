/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.TemplateLicense;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;

import java.util.List;

public interface TemplateLicenseRepository extends JmixDataRepository<TemplateLicense, Long> {

    List<TemplateLicense> findAll();

    List<TemplateLicense> findByLicenseName(String licenseName);

    List<TemplateLicense> findByPriority(Integer priority);

    TemplateLicense findTemplateLicenseById(Long id);

    @Query("select t from TemplateLicense t where t not in :licenses")
    List<TemplateLicense> findAvailableLicenses(List<TemplateLicense> licenses);

    // Adapted to fetch templates used within a specific project via the UsageLicense link
    @Query("select distinct ul.template from UsageLicense ul join InventoryItem i on i.softwareComponent = ul.softwareComponent where i.project = :project")
    List<TemplateLicense> findTemplateLicensesByProject(Project project);
}