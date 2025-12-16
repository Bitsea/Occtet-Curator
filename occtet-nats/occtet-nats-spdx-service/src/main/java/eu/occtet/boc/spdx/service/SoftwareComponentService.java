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

import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.spdx.dao.SoftwareComponentRepository;
import eu.occtet.boc.spdx.factory.SoftwareComponentFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SoftwareComponentService {

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private SoftwareComponentFactory softwareComponentFactory;

    private static final Logger log = LogManager.getLogger(SoftwareComponentService.class);



    public SoftwareComponent getOrCreateSoftwareComponent(String softwareName, String version){
        List<SoftwareComponent> softwareComponent = softwareComponentRepository.findByNameAndVersion(
                softwareName, version);
        if(softwareComponent.isEmpty()) {
            return softwareComponentFactory.create(softwareName, version);
        } else {
            return softwareComponent.getFirst();
        }
    }

    public SoftwareComponent update(SoftwareComponent softwareComponent){
        return softwareComponentRepository.save(softwareComponent);
    }


}
