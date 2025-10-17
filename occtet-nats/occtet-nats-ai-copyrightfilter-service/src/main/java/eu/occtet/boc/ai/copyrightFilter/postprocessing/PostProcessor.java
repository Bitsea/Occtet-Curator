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

package eu.occtet.boc.ai.copyrightFilter.postprocessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostProcessor {


    private static final Logger log = LogManager.getLogger(PostProcessor.class);
    private static final int thinkingCounter= 8;



    /**
     * models often have a <think>thinking part</think> in their response, here it gets deleted
     * @param response
     * @return
     */
    public String deleteThinking(String response){
        log.debug("delete thinking part: {}", response);
        String noThinking="";

        if (response.contains("<think>")) {
            noThinking = response.replace(response.substring(response.indexOf("<think>"), response.indexOf("</think>") + thinkingCounter), "");

            log.debug("NoThinking: {}", noThinking);
        } else noThinking= response;


        return noThinking;
    }

    /**
     * the String list is the concatenated responses of the ai, here the responses get cleaned
     * and copyrights getting separately into a new list
     * @param result
     * @return list of copyright strings
     */
    public List<String> cleanResults(String result){
        try {

            // concatenate the responses
            List<String> copyrights = new ArrayList<>();

            //remove start and end brackets in sub responses to have a valid json at the end
            String[] resultList = result.split("\\|\\|\\|");
            for (String r : resultList) {
                r = r.trim().replace("\n", "");
                if (!r.isEmpty())
                    copyrights.add(r);
            }

            List<String> noDuplicates= copyrights.stream().distinct().toList();
            List<String> finalList = new ArrayList<>();
           for(String s: noDuplicates) {
               if(!(s.length()<2)) {
                   log.debug("String added '{}'",s);
                   finalList.add(s);
               }
           }
           log.debug("final list size {}", finalList.size());
            return finalList;
        }catch(Exception e){
            log.error("result could not be processed: {}", e.getMessage());
            return null;
        }
    }

}
