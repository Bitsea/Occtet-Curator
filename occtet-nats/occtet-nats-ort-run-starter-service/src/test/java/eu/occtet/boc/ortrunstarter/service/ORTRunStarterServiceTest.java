package eu.occtet.boc.ortrunstarter.service;

import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

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

    private ORTRunStarterService ortRunStarterService = new ORTRunStarterService();


    //@Test // commented out because it requires a running ORT server and Keycloak instance on localhost.
    public void testStartOrtRun() throws IOException, InterruptedException, ApiException/* throws ApiException*/ {
        ortRunStarterService.startOrtRun(1);

    }
}