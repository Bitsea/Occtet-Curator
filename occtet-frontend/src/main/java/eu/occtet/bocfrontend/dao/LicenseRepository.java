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

import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;

import java.util.List;


public interface LicenseRepository  extends JmixDataRepository<License, Long> {

    List<License> findAll();
    List<License> findByLicenseName(String licenseName);
    List<License> findLicensesByCurated(Boolean curated);
    List<License> findLicensesByPriority(Integer priority);
    List<License> findLicensesByLicenseName(String licenseName);
    @Query("select distinct l from InventoryItem i join i.project p join i.softwareComponent sc join sc.licenses l where p = :project")
    List<License> findLicensesByProject(Project project);
    License findLicenseById(Long id);
}
