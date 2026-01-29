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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromptFactory {
    private static final Logger log = LogManager.getLogger(PromptFactory.class);


    /**
     * creating a specific prompt for the ai,
     * to discern false copyrights and put them into a output list
     * @return
     */
    public Prompt createFalseCopyrightPrompt(String copyrights ){
        try {

            String enrichedUserMessage = """
                    Copyrights:
                    %s

                    """.formatted(
                    copyrights
            );


            Message userMsg = new UserMessage(enrichedUserMessage);

            Prompt prompt = new Prompt( List.of( userMsg));
            log.debug("created prompt");
            return prompt;
        }catch(Exception e){
            log.error("There is an error in prompting {}", e.getMessage());
            return null;
        }


    }


}
