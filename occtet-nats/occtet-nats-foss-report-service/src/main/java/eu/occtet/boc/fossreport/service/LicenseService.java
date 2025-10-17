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

package eu.occtet.boc.fossreport.service;


import eu.occtet.boc.entity.License;
import eu.occtet.boc.fossreport.dao.LicenseRepository;
import eu.occtet.boc.fossreport.factory.LicenseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    @Autowired
    private LicenseFactory licenseFactory;

    @Autowired
    private LicenseRepository licenseRepository;

    public License findOrCreateLicenseWithModified(String licenseId, String licenseText, Boolean modified){
        List<License> license = licenseRepository.findByLicenseType(licenseId);

        if (!license.isEmpty()) {
            return license.getFirst();
        } else {

            return licenseFactory.createWithModified(licenseId, licenseText, modified);
        }
    }
}
