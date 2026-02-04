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

package eu.occtet.boc.ai.copyrightFilter.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {


    private static final Logger log = LogManager.getLogger(ChatClientConfig.class);

    /**
     * very basic config of the Chatclient, system prompt is defined here as a rule set
     * @param builder
     * @return
     */
    @Bean(name= "chatClient")
    public ChatClient chatClient(ChatClient.Builder builder) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        MessageChatMemoryAdvisor chatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory);
        return builder.defaultSystem("You must provide precise and concise answers."+
                        "You must find invalid copyrights from the list given by the user."+
                        "You must only answer with a list of invalid copyrights. You must accumulate these invalid copyrights in your output list." +
                        "You must separate individual invalid copyrights by this sign: ||| ."+
                        "It is required that you use your advisors to memorize and to get the good and bad examples documents from the vector database." +
                        "In doubt of validity or invalidity, dismiss object." +
                        "You must not add any additional explanation, reflection, thinking or any other additional comment to your output." +
                        "You must ensure that the invalid copyrights are unique in your list."+
                        "Only natural languages are used in valid copyrights, programming language is invalid."+
                        "Statements written in HMTL, Typescript, CSS or other programming languages are not valid copyrights." +
                        "valid copyright objects have copyright and/or copyright sign and/or (c) at first, followed by valid year date with one year or a span of years similar such as for example 2000-2019." +
                        "After that a valid personal name, which can have a forename and/or a surname, and/or a brand name and/or a company name. Without a year or time span the copyright is still valid."+
                        "Only use the copyrights from the user list for the final invalid copyright list. Do not use copyrights from your vector store for this list."+
                        "If you do not find invalid copyrights in the input list, add the sign: |||.")
                .defaultAdvisors(chatMemoryAdvisor).build();
    }


}
