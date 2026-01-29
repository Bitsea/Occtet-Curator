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

package eu.occtet.boc.ai.licenseMatcher.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {


    private static final Logger log = LoggerFactory.getLogger(ChatClientConfig.class);

    /**
     * very basic config of the Chatclient, most config is done in the Promptfactory as of now
     * @param builder
     * @return
     */
    @Bean(name= "chatClient")
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean(name="judgeClient")
    public ChatClient judgeClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        You are a deterministic license compliance judge.

                        Decide MATCH or NO MATCH only.

                        MATCH only if:
                            - all differences are inside SPDX optional or replaceable blocks
                            - no required text missing

                        Otherwise NO MATCH.

                        Output only MATCH or NO MATCH.
                        """)
                .build();
    }


}
