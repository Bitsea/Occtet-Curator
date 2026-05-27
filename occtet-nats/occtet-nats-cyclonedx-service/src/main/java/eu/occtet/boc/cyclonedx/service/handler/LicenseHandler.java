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

package eu.occtet.boc.cyclonedx.service.handler;

import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.cyclonedx.context.CycloneDxImportContext;
import eu.occtet.boc.cyclonedx.service.LicenseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.LicenseChoice;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.model.v2.license.*;
import org.spdx.library.model.v3_0_1.expandedlicensing.ListedLicense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LicenseHandler {

    @Autowired
    private LicenseService licenseService;

    private static final Logger log = LogManager.getLogger(LicenseHandler.class);


    public Set<SoftwareComponentLicenseUsage> createUsageLicenses(LicenseChoice licenseChoice,
                                                                  CycloneDxImportContext context,
                                                                  SoftwareComponent softwareComponent, Organization organization)
            throws InvalidSPDXAnalysisException {

        Map<String, License> licenseCache= context.getLicenseCache();
        Set<SoftwareComponentLicenseUsage> generatedUsages = new HashSet<>();
        List<AnyLicenseInfo> allLicenseInfo = new ArrayList<>();
        parseLicenseText(licenseChoice, allLicenseInfo);

        for (AnyLicenseInfo individualLicenseInfo : allLicenseInfo) {
            String licenseId = "";
            String licenseText;

            if (individualLicenseInfo instanceof SpdxListedLicense) {
                ListedLicense license = LicenseInfoFactory.getListedLicenseById(individualLicenseInfo.getId());
                licenseId = license.getId();
                licenseText = license.getLicenseText();
            } else if (individualLicenseInfo instanceof ExtractedLicenseInfo) {
                Optional<ExtractedLicenseInfo> extracted = licenseInfosExtractedSpdxDoc.stream()
                        .filter(s -> s.getLicenseId().equals(individualLicenseInfo.getId())).findFirst();
                if (extracted.isPresent()) {
                    licenseId = extracted.get().getId();
                    licenseText = extracted.get().getExtractedText();
                } else {
                    licenseText = "";
                }
            } else {
                licenseText = "";
            }

            if (licenseId.isEmpty()) licenseId = "Unknown";


            License licenseEntity = licenseCache.get(licenseId);

            if (licenseEntity == null) {
                licenseEntity = licenseCache.computeIfAbsent(licenseId,
                        id -> licenseService.findOrCreateTemplateLicense(id, licenseText));
                licenseCache.put(licenseId, licenseEntity);
            }

            SoftwareComponentLicenseUsage usage = new SoftwareComponentLicenseUsage();
            usage.setTemplate(licenseEntity);
            usage.setSoftwareComponent(softwareComponent);
            if (licenseText!= null && !licenseText.equals(licenseEntity.getLicenseText())) {
                usage.setUsageText(licenseText);
                usage.setIsModified(true);
            } else usage.setIsModified(false);
            if (licenseId!= null && !licenseId.equals(licenseEntity.getLicenseType())) {
                usage.setIsModified(true);
            }
            usage.setCustomName(licenseEntity.getLicenseName());
            usage.setCurated(false);
            usage.setOrganization(organization);

            generatedUsages.add(usage);


        }

        return generatedUsages;
    }

    private void parseLicenseText(LicenseChoice licenseChoice, List<AnyLicenseInfo> allLicenseInfos) throws InvalidSPDXAnalysisException {
        switch (licenseChoice) {
            case ConjunctiveLicenseSet conjunctiveLicenseSet -> {
                for (AnyLicenseInfo member : conjunctiveLicenseSet.getMembers()) {
                    parseLicenseText(member, allLicenseInfos);
                }
            }
            case DisjunctiveLicenseSet disjunctiveLicenseSet -> {
                for (AnyLicenseInfo member : disjunctiveLicenseSet.getMembers()) {
                    parseLicenseText(member, allLicenseInfos);
                }
            }
            case WithExceptionOperator withExceptionOperator -> parseLicenseText(withExceptionOperator.getLicense(), allLicenseInfos);
            case SpdxListedLicense listed -> allLicenseInfos.add(listed);
            case ExtractedLicenseInfo extracted -> allLicenseInfos.add(extracted);
            //No action needed if there is no license
            case SpdxNoneLicense ignored -> {
            }
            case SpdxNoAssertionLicense ignored -> {
            }
            case null, default -> log.info("Encountered unknown license type: {}", licenseChoice);
        }
    }


}
