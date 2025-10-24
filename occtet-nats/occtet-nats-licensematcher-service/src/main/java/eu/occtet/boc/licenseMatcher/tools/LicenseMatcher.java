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

package eu.occtet.boc.licenseMatcher.tools;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.library.ListedLicenses;
import org.spdx.library.model.v3_0_1.expandedlicensing.ListedLicense;
import org.spdx.utility.compare.CompareTemplateOutputHandler;
import org.spdx.utility.compare.LicenseCompareHelper;
import org.spdx.utility.compare.SpdxCompareException;
import org.springframework.stereotype.Component;

@Component
public class LicenseMatcher {

    private static final Logger log = LoggerFactory.getLogger(LicenseMatcher.class);


    /**
     * The spdxCompareLicense method compares the provided licenseText with
     * the SPDX standard license text associated with the given licenseId.
     * It uses the SPDX library to retrieve the standard license text and compares it with the provided text.
     * returns difference message
     * an InvalidSPDXAnalysisException occurs, if license text not matching
     * @param licenseId
     * @param licenseText
     * @return
     * @throws SpdxCompareException
     */
    public CompareTemplateOutputHandler.DifferenceDescription spdxCompareLicense(String licenseId, String licenseText){

        CompareTemplateOutputHandler.DifferenceDescription result = null;
        try {
            ListedLicense license = ListedLicenses.getListedLicenses().getListedLicenseById(licenseId);

            result = LicenseCompareHelper.isTextStandardLicense(license, licenseText);


        } catch (Exception e) {
            log.error("license match not working {}", e.getMessage());
        }
        return result;
    }

}
