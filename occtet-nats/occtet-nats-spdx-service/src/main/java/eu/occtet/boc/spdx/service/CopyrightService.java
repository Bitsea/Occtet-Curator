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


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.spdx.dao.CopyrightRepository;
import eu.occtet.boc.spdx.factory.CopyrightFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CopyrightService {


    @Autowired
    private CopyrightFactory copyrightFactory;
    @Autowired
    private CopyrightRepository copyrightRepository;

    public Copyright findOrCreateCopyright(String copyrightString, List<CodeLocation> codeLocations){
        List<Copyright> copyrights = copyrightRepository.findByCopyrightText(copyrightString);
        Copyright copyright;
        if (copyrights.isEmpty()) {
            copyright = copyrightFactory.create(copyrightString, codeLocations);
        }else{
            copyright= copyrights.getFirst();
            for(CodeLocation cl: codeLocations) {
               if(! copyright.getCodeLocations().contains(cl)){
                   copyright.getCodeLocations().add(cl);
               }
            }
        }

        return copyright;
    }


}
