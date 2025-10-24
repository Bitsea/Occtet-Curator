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

package eu.occtet.boc.licenseMatcher.service;


import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.model.FossReportServiceWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;
import eu.occtet.boc.model.WorkTask;
import io.nats.client.JetStreamApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@SpringBootTest // FIXME can we make this run without firing up the whole application context?
//@AutoConfigureDataJpa
public class LicenseMatcherServiceTest {

    @Autowired
    private LicenseMatcherService licenseMatcherService;
    private static final Logger log = LoggerFactory.getLogger(LicenseMatcherServiceTest.class);



    //@Test
    void callLicenseMatcherWithMatchingLicenses() throws JetStreamApiException, IOException {
        log.debug("Starting test: callLicenseMatcherWithMatchingLicenses");
        License license1 = new License();
        license1.setLicenseType("MIT");
        license1.setLicenseText("\n" +
                "\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n" +
                "\n" +
                "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n" +
                "\n" +
                "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n");
        SoftwareComponent component = new SoftwareComponent();
        component.setLicenses(List.of(license1));
        Project project = new Project();
        InventoryItem item = new InventoryItem("InventoryName", project, component);
        item.setSoftwareComponent(component);
        ScannerSendWorkData workDataResponse = new ScannerSendWorkData(item.getId());
        licenseMatcherService.process(workDataResponse);
        log.debug("External Notes: " + item.getExternalNotes());
        assertNotNull(item.getExternalNotes());
        assertTrue(item.getExternalNotes().contains("MIT"));
    }

   // @Test
    void callLicenseMatcherHandlesFailure() throws JetStreamApiException, IOException {
        License license = new License();
        license.setLicenseType("InvalidLicense");
        license.setLicenseText("Invalid License Text");
        SoftwareComponent component = new SoftwareComponent();
        component.setLicenses(List.of(license));
        InventoryItem item = new InventoryItem();
        item.setSoftwareComponent(component);

        ScannerSendWorkData workDataResponse = new ScannerSendWorkData(item.getId());
        licenseMatcherService.process(workDataResponse);

        assertNotNull(item.getExternalNotes());
        assertTrue(item.getExternalNotes().contains("url not successfully for license"));
    }

   // @Test
    void callLicenseMatcherForMultipleLicenses() throws JetStreamApiException, IOException {
        License license1 = new License();
        license1.setLicenseType("MIT");
        license1.setLicenseText("\n" +
                "\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n" +
                "\n" +
                "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n" +
                "\n" +
                "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n");
        License license2 = new License();
        license2.setLicenseType("0BSD");
        license2.setLicenseText("\n" +
                "\n" +
                "BSD Zero Clause License\n" +
                "\n" +
                "Copyright (C) YEAR by AUTHOR EMAIL\n" +
                "\n" +
                "Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted.\n" +
                "\n" +
                "THE SOFTWARE IS PROVIDED \"AS IS\" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.\n");
        SoftwareComponent component = new SoftwareComponent();
        component.setLicenses(List.of(license1, license2));
        Project project = new Project();
        InventoryItem item = new InventoryItem("InventoryName", project, component);
        item.setSoftwareComponent(component);
        ScannerSendWorkData workDataResponse = new ScannerSendWorkData(item.getId());
        licenseMatcherService.process(workDataResponse);

        assertNotNull(item.getExternalNotes());
        assertTrue(item.getExternalNotes().contains("MIT"));
        assertTrue(item.getExternalNotes().contains("0BSD"));
    }
  //  @Test
    void callLicenseMatcherMultipleLicensesOneValidOneInvalid() throws JetStreamApiException, IOException {
        License license1 = new License();
        license1.setLicenseType("MIT");
        license1.setLicenseText("\n" +
                "\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n" +
                "\n" +
                "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n" +
                "\n" +
                "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n");
        License license2 = new License();
        license2.setLicenseType("invalid");
        license2.setLicenseText("invalid");
        SoftwareComponent component = new SoftwareComponent();
        component.setLicenses(List.of(license1, license2));
        Project project = new Project();
        InventoryItem item = new InventoryItem("InventoryName", project, component);
        item.setSoftwareComponent(component);
        ScannerSendWorkData workDataResponse = new ScannerSendWorkData(item.getId());
        licenseMatcherService.process(workDataResponse);

        assertNotNull(item.getExternalNotes());
        assertTrue(item.getExternalNotes().contains("MIT"));
        assertTrue(item.getExternalNotes().contains("url not successfully"));
    }
}
