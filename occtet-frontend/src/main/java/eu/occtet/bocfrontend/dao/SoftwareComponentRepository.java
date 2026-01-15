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


import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.Vulnerability;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;

import java.util.List;




public interface SoftwareComponentRepository extends JmixDataRepository<SoftwareComponent, Long> {

    SoftwareComponent findByName(String softwareComponentName);
    List<SoftwareComponent> findAll();
    List<SoftwareComponent> findSoftwareComponentsByCurated(Boolean curated);

    @Query("select sc from SoftwareComponent sc join sc.vulnerabilities v where v = :vuln")
    List<SoftwareComponent> findByVulnerability(Vulnerability vuln);
}
