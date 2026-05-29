/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.dao;

import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.SoftwareComponentLicenseUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SoftwareComponentLicenseUsageRepository extends JpaRepository<SoftwareComponentLicenseUsage, Long> {

    @Query("SELECT u FROM SoftwareComponentLicenseUsage u " +
            "JOIN FETCH u.template " +
            "WHERE u.softwareComponent = :softwareComponent AND u.template = :template")
    List<SoftwareComponentLicenseUsage> findAllBySoftwareComponentAndTemplate(
            @Param("softwareComponent") SoftwareComponent softwareComponent,
            @Param("template") License template
    );}
