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

package eu.occtet.boc.cyclonedx.factory;

import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.Organization;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.SoftwareComponentLicenseUsage;
import org.springframework.stereotype.Component;

@Component
public class SoftwareComponentLicenseUsageFactory {

    public SoftwareComponentLicenseUsage createSoftwareComponentLicenseUsage(License licenseEntity, SoftwareComponent softwareComponent,
                                                                             String licenseText, String licenseId, String licenseName, Organization organization){
        SoftwareComponentLicenseUsage usage = new SoftwareComponentLicenseUsage();
        usage.setTemplate(licenseEntity);
        usage.setSoftwareComponent(softwareComponent);
        if (licenseText != null && !licenseText.equals(licenseEntity.getLicenseText())) {
            usage.setUsageText(licenseText);
            usage.setIsModified(true);
        } else usage.setIsModified(false);
        if (licenseId != null && !licenseId.equals(licenseEntity.getLicenseType())) {
            usage.setIsModified(true);
        }
        if (licenseEntity.getLicenseName() != null && !licenseEntity.getLicenseName().isEmpty()) {
            usage.setCustomName(licenseEntity.getLicenseName());
        } else usage.setCustomName(licenseName);
        usage.setCurated(false);
        usage.setOrganization(organization);
        return usage;
    }
}
