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

package eu.occtet.boc.spdx.service.handler;

import eu.occtet.boc.dao.LicenseRepository;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.TemplateLicense;
import eu.occtet.boc.entity.UsageLicense;
import eu.occtet.boc.spdx.service.LicenseService;
import eu.occtet.boc.spdx.service.TemplateLicenseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private LicenseRepository licenseRepository;
    @Autowired
    private LicenseService licenseService;

    @Autowired
    private TemplateLicenseService templateLicenseService;

    private static final Logger log = LogManager.getLogger(LicenseHandler.class);


    public List<UsageLicense> createUsageLicenses(AnyLicenseInfo spdxLicenseInfo,
                                                  Map<String, TemplateLicense> licenseCache,
                                                  Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc)
            throws InvalidSPDXAnalysisException {

        List<UsageLicense> generatedUsages = new ArrayList<>();
        List<AnyLicenseInfo> allLicenseInfo = new ArrayList<>();
        parseLicenseText(spdxLicenseInfo, allLicenseInfo);

        for (AnyLicenseInfo individualLicenseInfo : allLicenseInfo) {
            String licenseId = "";
            String licenseText = "";
            boolean isListed = false;

            if (individualLicenseInfo instanceof SpdxListedLicense) {
                ListedLicense license = LicenseInfoFactory.getListedLicenseById(individualLicenseInfo.getId());
                licenseId = license.getId();
                licenseText = license.getLicenseText();
                isListed = true;
            } else if (individualLicenseInfo instanceof ExtractedLicenseInfo) {
                Optional<ExtractedLicenseInfo> extracted = licenseInfosExtractedSpdxDoc.stream()
                        .filter(s -> s.getLicenseId().equals(individualLicenseInfo.getId())).findFirst();
                if (extracted.isPresent()) {
                    licenseId = extracted.get().getId();
                    licenseText = extracted.get().getExtractedText();
                }
            }

            if (licenseId.isEmpty()) licenseId = "Unknown";

            TemplateLicense templateEntity = licenseCache.get(licenseId);

            if (templateEntity == null) {
                templateEntity = templateLicenseService.findOrCreateTemplateLicense(licenseId, licenseText, licenseId, isListed);
                licenseCache.put(licenseId, templateEntity);
            }

            UsageLicense usage = new UsageLicense();
            usage.setTemplate(templateEntity);
            usage.setUsageText(licenseText);
            usage.setModified(false);
            usage.setCurated(false);

            generatedUsages.add(usage);
        }

        return generatedUsages;
    }

    private void parseLicenseText(AnyLicenseInfo licenseInfo, List<AnyLicenseInfo> allLicenseInfos) throws InvalidSPDXAnalysisException {
        switch (licenseInfo) {
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
            case null, default -> log.info("Encountered unknown license type: {}", licenseInfo);
        }
    }


}
