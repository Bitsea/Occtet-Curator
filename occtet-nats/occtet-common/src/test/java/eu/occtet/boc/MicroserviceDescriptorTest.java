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

package eu.occtet.boc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.UsageType;
import org.junit.Assert;
import org.junit.Test;

public class MicroserviceDescriptorTest {

    @Test
    public void testMicroserviceDescriptor() throws JsonProcessingException {
        MicroserviceDescriptor md = new MicroserviceDescriptor("name","description","version",
                "someWorkData", UsageType.RUNNABLE_BY_SERVICE);
        String result = (new ObjectMapper()).writerFor(MicroserviceDescriptor.class).writeValueAsString(md);
        Assert.assertEquals("{\"type\":\"descriptor\",\"name\":\"name\",\"description\":\"description\",\"version\":\"version\",\"acceptableWorkData\":\"someWorkData\",\"usageType\":\"RUNNABLE_BY_SERVICE\"}",result);
    }

}
