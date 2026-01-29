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

package eu.occtet.boc.ai.licenseMatcher.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromptFactory {
    private static final Logger log = LoggerFactory.getLogger(PromptFactory.class);


    public Prompt createLicenseMatcherPrompt( String url, String licenseText, String differenceDescription, String lineNumbers) {
        try {
            String enrichedUserMessage = """
                    SPDX_URL:
                    %s
                    
                    LICENSE_TEXT_FROM_PROJECT:
                    %s
                    
                    DifferenceDescription:
                    %s
                    
                    DifferenceLines:
                    %s

                    """.formatted(
                    url,
                    licenseText,
                    differenceDescription,
                    lineNumbers
            );


            Message userMsg = new UserMessage(enrichedUserMessage);

            String systemText = """
                    You are a deterministic SPDX license matching engine, not a chat assistant.
                    Your task is to compare LICENSE_TEXT_FROM_PROJECT against the SPDX standardLicenseTemplate retrieved from SPDX_URL.
                    YOu must only use english for responses.
                     The SPDX standardLicenseTemplate is the single source of truth.
                    
                     You MUST follow the steps below exactly.
                    
                     ================================================
                     PROCESS
                    
                     1. Retrieve SPDXLicenseDetail from SPDX_URL using your tool.
                     2. Extract standardLicenseTemplate.
                     3. Compare LICENSE_TEXT_FROM_PROJECT with standardLicenseTemplate.
                     4. Quote the licenseId from SPDXLicenseDetail.
                     5. Identify concrete textual differences.
                     6. Produce final verdict: MATCH or NO MATCH.
                     ================================================
                   
                    SPDX TEMPLATE MATCHING RULES:
                    
                     You must compare LICENSE_TEXT against standardLicenseTemplate.
                    
                     Only two kinds of differences are allowed:
                    
                     --------------------------------
                     OPTIONAL TEXT (<beginOptional> … </endOptional>)
                    
                     - Text inside <beginOptional> blocks may be present or absent.
                     - If present, it must match exactly.
                     - If absent, it is ignored.
                     - Optional text causes NO MATCH if present and not exactly matching.
                    
                     --------------------------------
                     REPLACEABLE TEXT (<var …> … </var>)
                    
                     - Text inside <var> blocks may differ.
                     - Differences are allowed only if they match the regex in the "match" attribute.
                     - Only the variable region may differ.
                     - Surrounding text must match exactly.
                     - If replacement does not satisfy regex → NO MATCH.
                    
                     --------------------------------
                     ALL OTHER TEXT
                    
                     - Must match verbatim.
                     - Must appear in the same order.
                     - Extra text outside optional/var blocks → NO MATCH.
                     - Missing required text → NO MATCH.
                     - Reordered paragraphs → NO MATCH.
                     
                     --------------------------------
                      HEURISTIC DIFFERENCE HINTS
                    
                      You are given:
                    
                      - DifferenceDescription
                      - DifferenceLines
                    
                      This is a heuristic result from a rule-based matcher.
                    
                      They:
                      - may be incomplete
                      - may contain false positives
                      - may miss real differences
                    
                      Use this only to help locate suspicious passages.
                    
                      You MUST still perform full comparison against standardLicenseTemplate.
                    
                      Final verdict must be based solely on SPDX template rules.
                     
                     --------------------------------
                      DECISION
                    
                      Final verdict:
                    
                      MATCH only if:
                      - All differences lie fully inside <var> or missing <optional> AND satisfy rules.
                    
                      Otherwise:
                      NO MATCH.
                      
                      ---------------------------------
                      
                   
                     Never summarize.
                     Never invent text.
                     Never use HTML.
                   \s""";


            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
            Message systemMessage = systemPromptTemplate.createMessage(Map.of("url", url));
            Prompt prompt = new Prompt(List.of(systemMessage, userMsg));
            log.debug("created prompt with url {}", url);
            return prompt;
        }catch(Exception e){
            log.error("There is an error in prompting {}", e.getMessage());
            return null;
        }




    }

}
