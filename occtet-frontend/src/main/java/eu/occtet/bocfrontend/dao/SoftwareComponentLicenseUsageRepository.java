/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https:www.apache.orglicensesLICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *   License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponentLicenseUsage;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SoftwareComponentLicenseUsageRepository extends JmixDataRepository<SoftwareComponentLicenseUsage, Long> {

    List<SoftwareComponentLicenseUsage> findAll();

    @Query("select lu from InventoryItem i " +
            "join i.softwareComponent sc " +
            "join sc.usageLicenses lu " +
            "where i.project = :project and lu.isModified = :modified")
    List<SoftwareComponentLicenseUsage> findByProjectAndModified(@Param("project") Project project,
                                                                 @Param("modified") Boolean modified);

    @Query("select ul from SoftwareComponentLicenseUsage ul join InventoryItem i on i.softwareComponent = ul.softwareComponent where i = :item")
    List<SoftwareComponentLicenseUsage> findByInventoryItem(InventoryItem item);


    List<SoftwareComponentLicenseUsage> findByIsModified(Boolean isModified);
}