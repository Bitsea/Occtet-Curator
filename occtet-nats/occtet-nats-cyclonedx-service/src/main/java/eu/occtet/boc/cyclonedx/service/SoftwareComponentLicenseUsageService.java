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
        // load existing usages for template and component
        log.debug("fetching usageLicense from DB");
        List<SoftwareComponentLicenseUsage> usageList = softwareComponentLicenseUsageRepository
                .findAllBySoftwareComponentAndTemplate(softwareComponent, licenseEntity);

        if (!usageList.isEmpty()) {
            log.debug("see if existing license matches name and text of already existing usageLicense");
            Optional<SoftwareComponentLicenseUsage> matchingUsage = usageList.stream()
                    .filter(usage -> {

                        //text matching
                        String existingText = usage.getUsageText() != null ? usage.getUsageText().trim() : "";
                        String incomingText = licenseText != null ? licenseText.trim() : "";
                        String templateText = licenseEntity.getLicenseText() != null ? licenseEntity.getLicenseText().trim() : "";

                        // is incoming text empty or only generic name?
                        boolean incomingTextIsDefault = incomingText.isEmpty()
                                || incomingText.equalsIgnoreCase(licenseId)
                                || incomingText.equalsIgnoreCase(licenseName)
                                || (licenseEntity.getLicenseName() != null && incomingText.equalsIgnoreCase(licenseEntity.getLicenseName().trim()));

                        boolean textMatch;
                        if (incomingTextIsDefault) {
                            textMatch = existingText.isEmpty() || existingText.equals(templateText);
                        } else {
                            textMatch = existingText.equals(incomingText);
                        }

                        // name matching
                        String existingName = usage.getCustomName() != null ? usage.getCustomName().trim() : "";
                        String incomingName = (licenseName != null && !licenseName.equals("null")) ? licenseName.trim() : "";
                        String templateName = licenseEntity.getLicenseName() != null ? licenseEntity.getLicenseName().trim() : "";

                        //is incoming name empty or is it the id?
                        boolean incomingNameIsDefault = incomingName.isEmpty() || incomingName.equalsIgnoreCase(licenseId);

                        boolean nameMatch;
                        if (incomingNameIsDefault) {
                            // if no name provided, match empty DB field OR template name
                            nameMatch = existingName.isEmpty()
                                    || existingName.equalsIgnoreCase(templateName)
                                    || existingName.equalsIgnoreCase(licenseId);
                        } else {
                            // if no custom name, it has to match
                            nameMatch = existingName.equalsIgnoreCase(incomingName);
                        }

                        return textMatch && nameMatch;
                    })
                    .findFirst();

            if (matchingUsage.isPresent()) {
                return matchingUsage.get();
            }
        }

        // if not existing: new
        log.debug("create new LicenseUsage");
        return softwareComponentLicenseUsageFactory.createSoftwareComponentLicenseUsage(
                licenseEntity, softwareComponent, licenseText, licenseId, licenseName, organization);
    }


}
