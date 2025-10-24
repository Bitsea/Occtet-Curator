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

package eu.occtet.boc.ai.licenseMatcher.postprocessor;


import eu.occtet.boc.ai.copyrightFilter.postprocessing.PostProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

//@SpringBootTest
//@AutoConfigureDataJpa
// NOTICE: If you comment out the next two lines, the test will use the DB connection configured in test/resources/application.properties
//@AutoConfigureTestEntityManager
//@AutoConfigureTestDatabase
public class CopyrightModelPostProcessorTest {

    private static final Logger log = LogManager.getLogger(CopyrightModelPostProcessorTest.class);


    @Autowired
    private PostProcessor postProcessor;

    //@Test
    public void createYamlFileTest() {
        String json = " ||| Copyright (c) today.year INRIA, France Telecom |||,  ||| Copyright (c) today.year INRIA, France Telecom |||,  ||| Copyright (c) today.year INRIA, France Telecom |||,  ||| Copyright (c) 219 INRIA, France Telecom |||";
        List<String> jsonString1 = postProcessor.cleanResults(json);
        assertEquals(2, jsonString1.size());
    }

    /**
     * testing if | in middle of copyright gets ignored
     */
    //@Test
    public void cleanResultsTest(){

        String jsonString1 = " ||| Copyright (c) today.year INRIA, France Telecom |||,  |||Copyright (c) today.year INRIA, France Telecom |||,  ||| Copyright (c) 16 INRIA, France Telecom |||,  ||| \nCopyright (c) 219 INRIA, France Telecom ||| \n \n |||";

        List<String> result = postProcessor.cleanResults(jsonString1);
        assertEquals(3, result.size());
        for(String s : result){
            log.debug("element {}", s);
        }
        assertEquals(1, result.stream().filter(s -> s.equals("Copyright (c) 219 INRIA, France Telecom")).count());
        assertEquals(1,result.stream().filter(s -> s.equals("Copyright (c) today.year INRIA, France Telecom")).count());


    }

    //@Test
    public void deleteThinkingTest(){

        String thinking= """
                 <think>
                Okay, let's start by understanding the user's query. They want me to identify invalid copyrights from a given list and present them in a specific format. The key points are that valid copyrights must include "(c)" followed by a year (or range), then either a personal name or entity like INRIA, France Telecom.
                
                First, I need to parse through each copyright entry the user provided. Let's look at them one by one:
                
                1. `Copyright (c) 2014 Mark Adler`
                   - Here, "(c)" is correct, followed by a year "2014", and then a personal name "Mark Adler". This seems valid.
                
                2. `Copyright (c) 2000-2011 INRIA, France Telecom`
                   - The copyright symbol is present, the year range is acceptable, and "INRIA" is an entity. So this looks good too.
                
                3. `Copyright (c) INRIA, France Telecom`
                   - Missing a year here. According to the rules, valid copyrights need a year or date. Without it, this should be invalid. Also, just checking entities without a year doesn't meet the criteria.
                
                4. `Copyright (c) 2000-2011 Gibberish`
                   - Has "(c)", year range, but "Gibberish" is likely not an actual entity. The user's examples mention that if there's doubt about the validity of the name or entity, it should be dismissed as invalid.
                
                5. `Copyright (c) 21 INRIA, France Telecom`
                   - Year here is just "21", which is ambiguous and doesn't specify a full year. This lacks clarity required by copyright standards, so it's likely invalid.
                
                Now, putting this together step-by-step:
                
                - The first two entries are valid because they have the proper symbols, years (or ranges), and recognized entities.
                - The third entry misses the year requirement.
                - The fourth uses an unrecognized entity ("Gibberish"), making it doubtful. So following the user's instruction to dismiss in doubt cases, this is invalid.
                - The fifth has a vague year "21", which doesn't meet the criteria of being a specific year or range.
                
                So compiling these into the required format with each invalid entry separated by ||| and ensuring they are unique leads us to the two entries: `Copyright (c) INRIA, France Telecom` and `Copyright (c) 2000-2011 Gibberish`. The rest are valid based on the given examples.
                
                The user might be concerned about legal validity in their codebase or documentation. They probably need to ensure compliance with copyright laws when using open-source libraries. By highlighting these specific invalid formats, they can avoid potential issues. Their deeper need is likely ensuring proper attribution and avoiding legal pitfalls by adhering strictly to recognized copyright formats.
                </think>
                |||Copyright (c) INRIA, France Telecom|||
                |||Copyright (c) 2000-2011 Gibberish|||""";

        String response = "";
        response = postProcessor.deleteThinking(thinking);

        log.debug("thinking deleted? : {}", response);
        assertFalse(response.contains("</think>") || response.contains("<think>"));



    }
}
