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

package eu.occtet.boc.cyclonedx.service;

import eu.occtet.boc.cyclonedx.factory.SoftwareComponentLicenseUsageFactory;
import eu.occtet.boc.dao.SoftwareComponentLicenseUsageRepository;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.Organization;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.SoftwareComponentLicenseUsage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SoftwareComponentLicenseUsageService {

    private static final Logger log = LogManager.getLogger(SoftwareComponentLicenseUsageService.class);
    @Autowired
    private SoftwareComponentLicenseUsageRepository softwareComponentLicenseUsageRepository;
    @Autowired
    private SoftwareComponentLicenseUsageFactory softwareComponentLicenseUsageFactory;

    public SoftwareComponentLicenseUsage createOrFindSoftwareComponentLicenseUsage(License licenseEntity, SoftwareComponent softwareComponent,
                                                                                   String licenseText, String licenseId, String licenseName, Organization organization) {

        List<SoftwareComponentLicenseUsage> usageList = softwareComponentLicenseUsageRepository.findAllBySoftwareComponentAndTemplate(softwareComponent, licenseEntity);
        if (!usageList.isEmpty() ){
            Optional<SoftwareComponentLicenseUsage> matchingUsage = usageList.stream()
                    .filter(usage ->
                                    Objects.equals(usage.getEffectiveText(), licenseText) &&
                                    Objects.equals(usage.getEffectiveName(), licenseName) &&
                                    Objects.equals(usage.getTemplate().getLicenseType(), licenseId)
                    )
                    .findFirst();


            return matchingUsage.orElseGet(() -> softwareComponentLicenseUsageFactory.createSoftwareComponentLicenseUsage(licenseEntity, softwareComponent, licenseText, licenseId, licenseName, organization));

        }else {

            return softwareComponentLicenseUsageFactory.createSoftwareComponentLicenseUsage(licenseEntity, softwareComponent, licenseText, licenseId, licenseName, organization);
        }
    }




}
