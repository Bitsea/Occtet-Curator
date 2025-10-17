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

package eu.occtet.boc.copyrightFilter.factory;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String createFalseCopyrightPrompt( ){


            return "Find invalid copyrights from the list given by the user."+
                    "A list of these invalid copyrights is your only output. Accumulate these invalid copyrights in your output list." +
                    "Compare with the good and bad examples from documents with context copyright." +
                    "In doubt of validity or invalidity, dismiss object." +
                    "Separate individual invalid copyrights by this sign: ||| . Do not add any additional explanation, reflection, thinking or any other additional comment to your output." +
                    "Each invalid copyright should be unique. Only natural languages are used in valid copyrights, programming language is invalid. Statements written in HMTL, Typescript, CSS or other programming languages are not valid copyrights." +
                    "Only use the copyrights from the user list for the final invalid copyright list. Do not use copyrights from your vector store for this list."+
                    "If you do not find invalid copyrights in the input list, add the sign: |||.";


    }

}
