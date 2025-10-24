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

import java.util.Optional;

@Service
public class CopyrightService {

    private static final Logger log = LogManager.getLogger(CopyrightService.class);

    @Autowired
    private CopyrightFactory copyrightFactory;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private CodeLocationService codeLocationService;

    public Copyright findOrCreateCopyright(String copyrightString, CodeLocation codeLocation){
        Optional<Copyright> copyright = copyrightRepository.findByCopyrightTextAndCodeLocation(copyrightString,
                codeLocation);
        if (!copyright.isPresent()) {
            copyright = Optional.ofNullable(copyrightFactory.create(copyrightString, codeLocation));
        }

        return copyright.get();
    }


}
