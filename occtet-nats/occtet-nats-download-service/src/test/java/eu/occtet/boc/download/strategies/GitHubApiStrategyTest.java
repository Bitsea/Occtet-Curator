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

package eu.occtet.boc.download.strategies;

import eu.occtet.boc.download.utils.DownloadUtils;
import eu.occtet.boc.download.utils.VersionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest
@ContextConfiguration(classes = {GitHubApiStrategy.class, GitHubApiStrategyTest.TestConfig.class})
class GitHubApiStrategyTest {

    @Autowired
    private GitHubApiStrategy strategy;

    @Autowired
    private MockRestServiceServer server;

    @MockitoBean
    private DownloadUtils downloadUtils;

    @MockitoBean
    private VersionUtils versionUtils;

    @TestConfiguration
    static class TestConfig {
        @Bean("gitHubRestClient")
        public RestClient gitHubRestClient(RestClient.Builder builder) {
            return builder.build();
        }
    }

    @Test
    void download_SuccessfulFlow_ShouldFindTagAndDownload() throws Exception {
        // Arrange
        URL url = new URI("https://github.com/user/repo.git").toURL();
        String version = "1.0";
        String expectedApiUrl = "/repos/user/repo/tags?per_page=100";
        String jsonResponse = """
            [
                {"name": "v1.0", "zipball_url": "https://api.github.com/zip/v1.0"},
                {"name": "v0.9", "zipball_url": "https://api.github.com/zip/v0.9"}
            ]
        """;

        this.server.expect(requestTo(containsString(expectedApiUrl)))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        when(versionUtils.isMatch("v1.0", "1.0")).thenReturn(true);

        strategy.download(url, version, Path.of("."));

        verify(downloadUtils).downloadFile("https://api.github.com/zip/v1.0", ".zip");
    }

    @Test
    void download_ShouldThrowException_WhenRepoNotFound() throws Exception {
        URL url = new URI("https://github.com/user/missing-repo").toURL();
        String expectedApiUrl = "/repos/user/missing-repo/tags";

        // Expect API Call -> Return 404
        this.server.expect(requestTo(containsString(expectedApiUrl)))
                .andRespond(withResourceNotFound()); // 404

        assertThrows(IOException.class, () ->
                strategy.download(url, "1.0", Path.of("."))
        );
    }

    @Test
    void download_ShouldThrowException_WhenNoMatchingTagFound() throws Exception {
        URL url = new URI("https://github.com/user/repo").toURL();
        String jsonResponse = "[{\"name\": \"v2.0\"}]";

        this.server.expect(requestTo(containsString("/repos/user/repo/tags")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        when(versionUtils.isMatch(anyString(), eq("1.0"))).thenReturn(false);

        assertThrows(IOException.class, () ->
                strategy.download(url, "1.0", Path.of("."))
        );
    }
}
