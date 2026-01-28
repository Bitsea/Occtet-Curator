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

package eu.occtet.boc.spdx.factory;


import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.dao.SoftwareComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoftwareComponentFactory {

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    public SoftwareComponent create(String softwareName, String version,
                                    List<License> license,  String url) {
        String purl="";
        boolean curated=false;
        SoftwareComponent softwareComponent= new SoftwareComponent(softwareName, version,license, url );
        return softwareComponentRepository.save(softwareComponent);}

    public SoftwareComponent create(String softwareName, String version) {
        return softwareComponentRepository.save(new SoftwareComponent(softwareName,version));
    }
}
