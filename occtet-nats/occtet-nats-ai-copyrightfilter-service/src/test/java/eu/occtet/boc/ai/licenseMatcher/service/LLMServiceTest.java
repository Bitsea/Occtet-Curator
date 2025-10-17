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

package eu.occtet.boc.ai.licenseMatcher.service;



import eu.occtet.boc.ai.copyrightFilter.AICopyrightFilterServiceApp;
import eu.occtet.boc.ai.copyrightFilter.postprocessing.PostProcessor;
import eu.occtet.boc.ai.copyrightFilter.service.LLMService;
import eu.occtet.boc.model.AIStatusQueryWorkData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = AICopyrightFilterServiceApp.class)
// NOTICE: If you comment out the next two lines, the test will use the DB connection configured in test/resources/application.properties
@AutoConfigureTestEntityManager
@AutoConfigureTestDatabase
public class LLMServiceTest {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceTest.class);


    @Autowired
    private LLMService llmService;

    @Autowired
    private ChatClient chatClient;


    @Autowired
    private PostProcessor postProcessor;



    //@Test // FIXME this is actually an integration test, not a unit test, because it requires a working LLM connection.
    // Try to mock the LLM connection or the AI
    public void generateTest() {
        //making sure the connection is working
        try {
            String result = chatClient.prompt().user("tell a joke").call().content();
            log.info(result);
            assertNotNull(result);

        }catch(Exception e){
            log.error("Error {}", e.getMessage());
        }

    }

    //@Test
    void askAIWithoutAnyAdvisorsToolsTest() {
        String copyright="Copyright (c) today.year INRIA, France Telecom";
        Prompt prompt = new Prompt((Message) List.of("valid copyright has elements at least two elements: Copyright sign or word and the copyright holder. Addtionally there can be a date", "Is this a valid copyright?: "+ copyright+ "If yes, return the copyright. If no, return 'false'.  Answer only with the valid copyright or 'false'"));
        AIStatusQueryWorkData workData= new AIStatusQueryWorkData();
        workData.setDetails("give status");
        llmService.process(workData);
        //FIXME answer of ai ?


    }
}
