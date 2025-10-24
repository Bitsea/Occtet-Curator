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

package eu.occtet.boc.ai.copyrightFilter.factory;

import eu.occtet.boc.ai.copyrightFilter.retriever.CopyrightRetriever;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdvisorFactory {
    @Autowired
    private CopyrightRetriever copyrightRetriever;


    public List<Advisor> createAdvisors(){
        List<Advisor> advisors = new ArrayList<>();

        //TODO figure out where we get topK and similarityThreshold from and find suitable values for our usecases
        //advisor for "good copyrights"
        advisors.add(copyrightRetriever.buildQuestionAnswerAdvisor(10, 0.25, "Examples of good copyrights: ", "fileName == 'good-copyrights.txt'"));
        //advisor for "bad copyrights"
        advisors.add(copyrightRetriever.buildQuestionAnswerAdvisor(10, 0.25, "Examples of bad copyrights: ", "fileName == 'bad-copyrights.txt'"));

        return advisors;
    }
}
