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

package eu.occtet.boc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;
import junit.framework.TestCase;
import org.junit.Test;

public class WorkDataProcessorTest extends TestCase {

    @Test
    public void testProcessSample() throws JsonProcessingException {
        SampleWorkDataProcessor processor = new SampleWorkDataProcessor();
        // Beispiel JSON-Nachricht für TypeAMessage
        String jsonMessageA = "{\"type\":\"sample\",\"sampleField\":\"WertA\"}";

        ObjectMapper objectMapper = new ObjectMapper();
        BaseWorkData data = objectMapper.readValue(jsonMessageA, BaseWorkData.class);

        boolean res = data.process(processor);

        assertTrue(res);
    }

    public void testProcessSampleScannerWorkData() throws JsonProcessingException {
        SampleWorkDataProcessor processor = new SampleWorkDataProcessor();

        // Beispiel JSON-Nachricht für TypeAMessage
        String jsonMessageA = "{\"type\":\"scannerdata_send\",\"inventoryItemId\":\"2bd05cfd-ebfb-4393-b0eb-faaff27801ab\"}}";

        ObjectMapper objectMapper = new ObjectMapper();
        BaseWorkData data = objectMapper.readValue(jsonMessageA, BaseWorkData.class);

        boolean res = data.process(processor);

        assertTrue(res);
    }
}