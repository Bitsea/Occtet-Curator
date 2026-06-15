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

import eu.occtet.boc.cyclonedx.service.SoftwareComponentLicenseUsageService;
import eu.occtet.boc.dao.LicenseRepository;
import eu.occtet.boc.dao.SoftwareComponentLicenseUsageRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.cyclonedx.context.CycloneDxImportContext;
import eu.occtet.boc.cyclonedx.service.LicenseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.license.Expression;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.model.v2.license.*;
import org.spdx.library.model.v3_0_1.expandedlicensing.ListedLicense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LicenseHandler {

    @Autowired
    private LicenseService licenseService;

    private static final Logger log = LogManager.getLogger(LicenseHandler.class);

    @Autowired
    private SoftwareComponentLicenseUsageService softwareComponentLicenseUsageService;



    public String createUsageLicenses(LicenseChoice licenseChoice,
                                                                  CycloneDxImportContext context,
                                                                  SoftwareComponent softwareComponent, Organization organization) {

        Map<String, License> licenseCache= context.getLicenseCache();

        StringBuilder licenseDeclaration= new StringBuilder();
        if (licenseChoice != null) {
            log.debug("Processing license choice for component {}: {}", softwareComponent.getName(), licenseChoice);
            if (licenseChoice.getExpression() != null) {
                Expression expression = licenseChoice.getExpression();
                licenseDeclaration.append(expression.getValue());
                String cleanedExpression = licenseDeclaration.toString().replaceAll("[()]", "");

                String[] licenses = cleanedExpression.split("\\s+(?i)(?:AND|OR)\\s+");

                for (String license : licenses) {
                    String licenseKeyId = license.trim();
                    String cacheKey = softwareComponent.getName() + "_" + licenseKeyId;

                    //see if already existing
                    if (context.getUsageLicenseCache().containsKey(cacheKey)) {
                        log.debug("Usage for component {} with license {} already in cache. Skipping.", softwareComponent.getName(), licenseKeyId);
                        continue;
                    }

                    SoftwareComponentLicenseUsage usage = createUsageForLicense(licenseKeyId, licenseKeyId, null, softwareComponent, organization, licenseCache);
                    context.getUsageLicenseCache().put(cacheKey, usage);
                }

            }else {

                for (org.cyclonedx.model.License lic : licenseChoice.getLicenses()) {
                    String licenseId = lic.getId();
                    if(licenseId.isEmpty()) licenseId = "Unknown";
                    String licenseName = lic.getName();
                    String licenseText = "";
                    if (lic.getAttachmentText() != null) {
                        AttachmentText attachment = lic.getAttachmentText();
                        String content = attachment.getText();


                        if (content != null) {
                            if ("base64".equalsIgnoreCase(attachment.getEncoding())) {
                                try {
                                    byte[] decodedBytes = Base64.getDecoder().decode(content.trim());
                                    licenseText = new String(decodedBytes, StandardCharsets.UTF_8);
                                } catch (IllegalArgumentException e) {
                                    licenseText = content;
                                }
                            } else {
                                licenseText = content;
                            }
                        }
                    }
                    if(licenseDeclaration.isEmpty()){
                        licenseDeclaration.append(licenseId);
                    }else licenseDeclaration.append(" AND ").append(licenseId);

                    String targetIdForKey = "Unknown".equals(licenseId) && licenseName != null ? licenseName : licenseId;
                    String cacheKey = softwareComponent.getName() + "_" + targetIdForKey;

                    // Check duplicate
                    if (context.getUsageLicenseCache().containsKey(cacheKey)) {
                        log.debug("Usage for component {} with license {} already in cache. Skipping.", softwareComponent.getName(), targetIdForKey);
                        continue;
                    }

                    SoftwareComponentLicenseUsage usage= createUsageForLicense(licenseId,licenseName,
                            licenseText,softwareComponent,organization,licenseCache);
                    context.getUsageLicenseCache().put(cacheKey, usage);
                }
            }


        }
        context.getLicenseCache().putAll(licenseCache);


        return licenseDeclaration.toString();
    }

    private SoftwareComponentLicenseUsage createUsageForLicense(String licenseId, String licenseName,  String licenseText,
                                                                SoftwareComponent softwareComponent,
                                                                Organization organization, Map<String, License> licenseCache) {
        License licenseEntity = licenseCache.get(licenseId);

        if (licenseEntity == null) {
            licenseEntity = licenseCache.computeIfAbsent(licenseId,
                    id -> licenseService.findOrCreateTemplateLicense(id, licenseText));
            licenseCache.put(licenseId, licenseEntity);
        }


        return softwareComponentLicenseUsageService.createOrFindSoftwareComponentLicenseUsage(licenseEntity, softwareComponent,
                licenseText, licenseId, licenseName, organization);

    }


}
