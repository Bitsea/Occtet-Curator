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

package eu.occtet.boc.spdx.service;



import eu.occtet.boc.entity.License;
import eu.occtet.boc.dao.LicenseRepository;
import eu.occtet.boc.spdx.factory.LicenseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LicenseService {

    private static final Logger log = LogManager.getLogger(LicenseService.class);


    @Autowired
    private LicenseFactory licenseFactory;

    @Autowired
    private LicenseRepository licenseRepository;



    public License findOrCreateLicense(String licenseId, String licenseText, String licenseName ) {
        log.debug("before getting licenses");
        List<License> license = licenseRepository.findByLicenseTypeAndLicenseText(licenseId, licenseText);
        log.debug("after getting licenses {}", license.size());
        if (!license.isEmpty()) {
            return license.getFirst();
        } else {
            List<License> licenses = licenseRepository.findByLicenseType(licenseId);
            if(!licenses.isEmpty()){
                if(!license.getFirst().getLicenseText().equals(licenseText)){
                    return licenseFactory.createWithName(licenseId, licenseText, licenseName+"-variant");
                }
            }
            return licenseFactory.createWithName(licenseId, licenseText, licenseName);
        }
    }

}
