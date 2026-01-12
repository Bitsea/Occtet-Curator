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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    /**
     * Finds or creates a batch of {@link Copyright} entities based on the provided set of copyright texts.
     * If a {@link Copyright} entity corresponding to a text already exists in the repository, it is retrieved.
     * Otherwise, a new transient entity is created, saved, and included in the result.
     *
     * @param copyrightTexts the set of copyright text strings to look up or create in batch
     * @return a map where the keys are the copyright text strings and the values are the corresponding {@link Copyright} entities
     */
    @Transactional
    public Map<String, Copyright> findOrCreateBatch(Set<String> copyrightTexts){
        Map<String, Copyright> cache = new HashMap<>();
        List<String> textList = new ArrayList<>(copyrightTexts);

        int batchSize = 1000;
        for (int i = 0; i < textList.size(); i += batchSize) {
            List<String> batch = textList.subList(i, Math.min(textList.size(), i + batchSize));
            List<Copyright> found = copyrightRepository.findByCopyrightTextIn(batch);
            found.forEach(c -> cache.put(c.getCopyrightText(), c));
        }

        List<Copyright> toSave = new ArrayList<>();
        for (String text : copyrightTexts) {
            if (!cache.containsKey(text)) {
                Copyright newCopyright = copyrightFactory.createTransient(text, new ArrayList<>());
                toSave.add(newCopyright);
                cache.put(text, newCopyright);
            }
        }

        if (!toSave.isEmpty()) {
            copyrightRepository.saveAll(toSave);
        }
        return cache;
    }

}
