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

package eu.occtet.boc.fossreport.factory;

import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.fossreport.dao.SoftwareComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoftwareComponentFactory {

    private static final Logger log = LoggerFactory.getLogger(SoftwareComponentFactory.class);

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    public SoftwareComponent create(String softwareName, String version,
                                              List<License> license, String url) {
        log.debug("Creating Software Component with name: {}, version: {} and more...", softwareName, version);
            return softwareComponentRepository.save(new SoftwareComponent(
                    softwareName,
                    version,
                    license,
                    url));
    }

    public SoftwareComponent create(String softwareName, String version) {
        log.debug("Creating Software Component with name: {} and version: {}", softwareName, version);

        return softwareComponentRepository.save(new SoftwareComponent(softwareName,version));
    }
}
