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

package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SPDXLicenseDetails{
        private boolean isOsiApproved;
        private boolean isDeprecatedLicenseId;
        private String licenseText;
        private String standardLicenseTemplate;
        private String name;
        private String licenseId;
        private String licenseTextHtml;

    public SPDXLicenseDetails() {
    }

    public SPDXLicenseDetails(boolean isOsiApproved, boolean isDeprecatedLicenseId, String licenseText, String standardLicenseTemplate, String name, String licenseId, String licenseTextHtml) {
        this.isOsiApproved = isOsiApproved;
        this.isDeprecatedLicenseId = isDeprecatedLicenseId;
        this.licenseText = licenseText;
        this.standardLicenseTemplate = standardLicenseTemplate;
        this.name = name;
        this.licenseId = licenseId;
        this.licenseTextHtml = licenseTextHtml;
    }

    public boolean isOsiApproved() {
        return isOsiApproved;
    }

    public void setOsiApproved(boolean osiApproved) {
        isOsiApproved = osiApproved;
    }

    public boolean isDeprecatedLicenseId() {
        return isDeprecatedLicenseId;
    }

    public void setDeprecatedLicenseId(boolean deprecatedLicenseId) {
        isDeprecatedLicenseId = deprecatedLicenseId;
    }

    public String getLicenseText() {
        return licenseText;
    }

    public void setLicenseText(String licenseText) {
        this.licenseText = licenseText;
    }

    public String getStandardLicenseTemplate() {
        return standardLicenseTemplate;
    }

    public void setStandardLicenseTemplate(String standardLicenseTemplate) {
        this.standardLicenseTemplate = standardLicenseTemplate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getLicenseTextHtml() {
        return licenseTextHtml;
    }

    public void setLicenseTextHtml(String licenseTextHtml) {
        this.licenseTextHtml = licenseTextHtml;
    }
}
