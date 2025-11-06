package eu.occtet.boc.ortrunstarter.service;

import junit.framework.TestCase;
import org.openapitools.client.ApiException;
import org.openapitools.client.model.Organization;

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


public class ORTRunStarterServiceTest extends TestCase {



    public void testGetOrganizations() throws ApiException {

        ORTRunStarterService service = new ORTRunStarterService();
        List<Organization> organizations = service.getOrganizations();
        for(Organization o : organizations) {
            System.out.println(o.toString());
        }


    }
}