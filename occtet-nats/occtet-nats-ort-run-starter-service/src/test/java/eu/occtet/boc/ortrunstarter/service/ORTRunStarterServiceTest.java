package eu.occtet.boc.ortrunstarter.service;

import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

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

@SpringBootTest
@ContextConfiguration(classes = {ORTRunStarterService.class})
public class ORTRunStarterServiceTest extends TestCase {

//    @Autowired
    private ORTRunStarterService ortRunStarterService = new ORTRunStarterService();


    @Test
    // NOTICE this is actually demo code at the moment.
    public void testGetOrganizations()/* throws ApiException*/ {
        ortRunStarterService.startOrtRun(1);
        /*List<Organization> organizations = ortRunStarterService.getOrganizations();
        for(Organization o : organizations) {
            System.out.println(o.toString());
        }*/
    }
}