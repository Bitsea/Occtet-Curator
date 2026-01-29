/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.ai.licenseMatcher.tools;

import eu.occtet.boc.model.SPDXLicenseDetails;
import eu.occtet.boc.ai.licenseMatcher.service.LicenseTemplateWebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * AI Tool to fetch license information from a given URL
 * here spdx URL to fetch spdx information for a license
 */
@Component
public class LicenseTool {

    private static final Logger log =LoggerFactory.getLogger(LicenseTool.class);


    @Tool(description = "Fetch SPDX license details. URL is REQUIRED.")
    public SPDXLicenseDetails getLicenseInformation(@ToolParam(description = "REQUIRED. Absolute URL to SPDX license details JSON. Must not be null.") String url){
        log.debug("using Licensetool with url {}", url);
        try {
            LicenseTemplateWebService ltWebService = new LicenseTemplateWebService();

            return ltWebService.readDefaultLicenseInfos(url);
        }catch(Exception e){
            log.error("String url {} could not be called, {}", url , e.getMessage());
            return null;
        }
    }
}
