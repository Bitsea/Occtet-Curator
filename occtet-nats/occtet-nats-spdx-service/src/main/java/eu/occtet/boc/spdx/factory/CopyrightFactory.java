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


import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.dao.CopyrightRepository;
import eu.occtet.boc.entity.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CopyrightFactory {

    @Autowired
    private CopyrightRepository copyrightRepository;

    public Copyright create(String copyrightString, Set<File> codeLocations){
        Copyright copyright = new Copyright(copyrightString, codeLocations);

        return copyrightRepository.save(copyright);
    }

    public Copyright createTransient(String copyrightString, Set<File> codeLocations){
        return new Copyright(copyrightString, codeLocations);
    }
}
