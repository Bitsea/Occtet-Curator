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

package eu.occtet.boc.spdx.service;

import eu.occtet.boc.dao.TemplateLicenseRepository;
import eu.occtet.boc.entity.TemplateLicense;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class TemplateLicenseService {

    private static final Logger log = LogManager.getLogger(TemplateLicenseService.class);

    @Autowired
    private TemplateLicenseRepository templateLicenseRepository;

    public TemplateLicense findOrCreateTemplateLicense(String licenseId, String templateText, String licenseName, boolean isSpdx) {
        log.debug("Finding or creating Template License for {}", licenseId);

        // Only check by ID/Type now, variations in text belong to UsageLicense
        List<TemplateLicense> templates = templateLicenseRepository.findByLicenseType(licenseId);

        if (!templates.isEmpty()) {
            return templates.getFirst();
        } else {
            TemplateLicense newTemplate = new TemplateLicense();
            newTemplate.setLicenseType(licenseId);
            newTemplate.setTemplateText(templateText);
            newTemplate.setLicenseName(licenseName);
            newTemplate.setIsSpdx(isSpdx);

            return templateLicenseRepository.save(newTemplate);
        }
    }
}
