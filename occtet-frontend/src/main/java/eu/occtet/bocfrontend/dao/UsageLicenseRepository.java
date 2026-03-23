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

import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.UsageLicense;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;

import java.util.List;

public interface UsageLicenseRepository extends JmixDataRepository<UsageLicense, Long> {

    List<UsageLicense> findAll();

    List<UsageLicense> findByCurated(Boolean curated);

    @Query("select distinct ul from UsageLicense ul join InventoryItem i on i.softwareComponent = ul.softwareComponent where i.project = :project")
    List<UsageLicense> findUsageLicensesByProject(Project project);

    @Query("select ul from UsageLicense ul join InventoryItem i on i.softwareComponent = ul.softwareComponent join ul.template t where i.project = :project and t.licenseName = :licenseName")
    List<UsageLicense> findUsageLicensesByLicenseNameAndProject(String licenseName, Project project);

    @Query("select ul from UsageLicense ul join InventoryItem i on i.softwareComponent = ul.softwareComponent where i = :item")
    List<UsageLicense> findByInventoryItem(InventoryItem item);

    UsageLicense findLicenseById(Object licenseId);
}
