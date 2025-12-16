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


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.fossreport.dao.CopyrightRepository;
import eu.occtet.boc.fossreport.factory.CopyrightFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CopyrightService {

    private static final Logger log = LoggerFactory.getLogger(CopyrightService.class);



    @Autowired
    private CopyrightFactory copyrightFactory;
    @Autowired
    private CopyrightRepository copyrightRepository;


    public Copyright findOrCreateCopyright(String copyrightString, CodeLocation codeLocation) {
        List<Copyright> copyright = copyrightRepository.findByCopyrightText(copyrightString);
        if (!copyright.isEmpty() && copyright.getFirst() != null) {
            if(copyright.getFirst().getCodeLocations()==null){
                copyright.getFirst().setCodeLocations(new ArrayList<>(List.of(codeLocation)));
            }else if( !copyright.getFirst().getCodeLocations().contains(codeLocation)) {
                copyright.getFirst().getCodeLocations().add(codeLocation);
            }
            return copyright.getFirst();
        }

        return copyrightFactory.create(copyrightString, List.of(codeLocation));
    }


}
