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


import eu.occtet.boc.ai.licenseMatcher.service.LicenseTemplateWebService;
import eu.occtet.boc.model.SPDXLicenseDetails;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


public class LicenseTemplateWebServiceTest {

    private static final Logger log = LoggerFactory.getLogger(LicenseTemplateWebServiceTest.class);


    public static MockWebServer mockBackEnd;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }


    //@Test
    public void testFetchDataFromWebSuccess() throws Exception {

        // also please always load the file from resources, like so:
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource("0BSD.json");
        assertNotNull(resource, "Resource file should exist");


        String licenseId = "0BSD";
        String json = readFileAsString(resource.getFile());
        log.debug(json);


        MockWebServer mockWebServer = new MockWebServer();
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());

        mockBackEnd.enqueue(new MockResponse()
                .setBody(json) //"{\"licenseText\":\"" +licenseText1+"\", \"name\":\""+ licenseName+"\", \"licenseId\": \"" +licenseId +"\"}")
                .addHeader("Content-Type", "application/json"));

        LicenseTemplateWebService licenseTemplateWebService = new LicenseTemplateWebService();
        SPDXLicenseDetails result = licenseTemplateWebService.readDefaultLicenseInfos(baseUrl);
        assertNotNull(result);
        assertEquals(licenseId, result.getLicenseId());


    }

    public static String readFileAsString(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    //@Test // FIXME consider if you use the same file in two tests, load it once during a setup function.
    public void testFetchDataFromWebFailure() throws Exception {

        String licenseId = "Unknown";
        String file = "src/test/resources/0BSD.json";
        String json = readFileAsString(file);
        log.debug(json);


        MockWebServer mockWebServer = new MockWebServer();
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());

        mockBackEnd.enqueue(new MockResponse()
                .setBody(json) //"{\"licenseText\":\"" +licenseText1+"\", \"name\":\""+ licenseName+"\", \"licenseId\": \"" +licenseId +"\"}")
                .addHeader("Content-Type", "application/json"));

        LicenseTemplateWebService licenseTemplateWebService = new LicenseTemplateWebService();
        SPDXLicenseDetails result = licenseTemplateWebService.readDefaultLicenseInfos(baseUrl);
        assertNotNull(result);
        assertNotEquals(licenseId, result.getLicenseId());


    }


}
