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

package eu.occtet.boc.ai.licenseMatcher.retriever;

import eu.occtet.boc.ai.copyrightFilter.retriever.CopyrightRetriever;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@SpringBootTest
//@AutoConfigureDataJpa
// NOTICE: If you comment out the next two lines, the test will use the DB connection configured in test/resources/application.properties
//@AutoConfigureTestEntityManager
//@AutoConfigureTestDatabase
public class CopyrightModelRetrieverTest {
    private static final Logger log = LogManager.getLogger(CopyrightModelRetrieverTest.class);

    @Autowired
    CopyrightRetriever copyrightRetriever;

    //@Test
    public void similaritySearchTest() {
        copyrightRetriever.loadVectorStore("bad copyright examples");
        List<Document> docs= copyrightRetriever.similaritySearch( 60, 0.5, "Get copyright examples", "fileName == 'bad-copyrights.txt'");
        for(Document d: docs){
            log.debug("META {}", d.getMetadata());
            log.debug("CONTENT {}", d.getFormattedContent());
        }
        //Here the output depends on how many files you already uploaded into the vector store
        assertEquals(2,docs.size());
    }

}