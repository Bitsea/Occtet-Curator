/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.model;

import eu.occtet.bocfrontend.entity.License;

import java.util.List;

public class SPDXLicenseInfos {

        private String licenseListVersion;
        private List<License> licenses;
        private String releaseDate;

        public String getLicenseListVersion() {
            return licenseListVersion;
        }

        public void setLicenseListVersion(String licenseListVersion) {
            this.licenseListVersion = licenseListVersion;
        }

        public List<License> getLicenses() {
            return licenses;
        }

        public void setLicenses(List<License> licenses) {
            this.licenses = licenses;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }

}
