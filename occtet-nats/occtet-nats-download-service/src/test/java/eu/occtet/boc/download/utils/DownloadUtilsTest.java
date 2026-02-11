/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.download.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(DownloadUtils.class)
@ContextConfiguration(classes = {DownloadUtils.class, DownloadUtilsTest.TestConfig.class})
class DownloadUtilsTest {

    @Autowired
    private DownloadUtils downloadUtils;

    @Autowired
    private MockRestServiceServer server;

    @TestConfiguration
    static class TestConfig {
        @Bean("genericRestClient")
        public RestClient genericRestClient(RestClient.Builder builder) {
            return builder.build();
        }
    }
    @Test
    void downloadFile_ShouldStreamContentToFile_AndCleanup() throws IOException {
        String url = "https://example.com/test.zip";
        byte[] content = "fake-zip-content".getBytes();

        this.server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_OCTET_STREAM));

        Path result = null;
        try {
            result = downloadUtils.downloadFile(url, ".zip");

            assertNotNull(result);
            assertTrue(Files.exists(result), "File should exist on disk");
            assertTrue(result.getFileName().toString().endsWith(".zip"));
            assertArrayEquals(content, Files.readAllBytes(result));
        } finally {
            if (result != null) {
                Files.deleteIfExists(result);
            }
        }
    }

    @Test
    void downloadFile_ShouldThrowIOException_WhenServerReturns404() {
        String url = "https://example.com/missing.zip";

        this.server.expect(requestTo(url))
                .andRespond(withResourceNotFound());

        assertThrows(IOException.class, () -> downloadUtils.downloadFile(url, ".zip"));
    }

    @Test
    void normalizeUrl_ShouldHandleGitSchemes() {
        assertEquals("https://github.com/user/repo", downloadUtils.normalizeUrl("git+ssh://github.com/user/repo"));
        assertEquals("https://github.com/user/repo", downloadUtils.normalizeUrl("ssh://github.com/user/repo"));
        assertEquals("https://github.com/user/repo", downloadUtils.normalizeUrl("git+https://github.com/user/repo"));
        assertEquals("http://github.com/user/repo", downloadUtils.normalizeUrl("git+http://github.com/user/repo"));
        assertEquals("https://github.com/user/repo", downloadUtils.normalizeUrl(" https://github.com/user/repo "));
        assertNull(downloadUtils.normalizeUrl(null));
    }

}