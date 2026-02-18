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

import com.github.packageurl.PackageURL;
import eu.occtet.boc.download.utils.DownloadUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DirectHttpStrategyTest {
    @Mock
    private DownloadUtils downloadUtils;

    @InjectMocks
    private DirectHttpStrategy strategy;

    @Test
    void canHandle_Url_ShouldAcceptHttpHttps_AndRejectGitHub() throws Exception {
        // Accept
        assertTrue(strategy.canHandle(new URI("https://example.com/file.zip").toURL(), "1.0"));
        assertTrue(strategy.canHandle(new URI("http://insecure.com/file").toURL(), "1.0"));

        // Reject
        assertFalse(strategy.canHandle(new URI("https://github.com/user/repo").toURL(), "1.0"));
        assertFalse(strategy.canHandle((URL) null, "1.0"));
    }

    @Test
    void canHandle_Purl_ShouldAcceptGenericOnly() throws Exception {
        // Accept
        assertTrue(strategy.canHandle(new PackageURL("pkg:generic/file@1.0")));
        assertTrue(strategy.canHandle(new PackageURL("pkg:maven/g/a@1.0?download_url=http://e.com")));

        // Reject (Maven without download_url)
        assertFalse(strategy.canHandle(new PackageURL("pkg:maven/g/a@1.0")));
    }

    @Test
    void download_Url_DelegatesToUtils() throws IOException, URISyntaxException {
        URL url = new URI("https://example.com/file.zip").toURL();

        strategy.download(url, "1.0", Path.of("."));

        verify(downloadUtils).downloadFile("https://example.com/file.zip");
    }

    @Test
    void download_Purl_ExtractsDownloadUrl() throws Exception {
        PackageURL purl = new PackageURL("pkg:generic/name@1.0?download_url=https://target.com/file");

        strategy.download(purl, Path.of("."));

        verify(downloadUtils).downloadFile("https://target.com/file");
    }

    @Test
    void download_Purl_ThrowsIfMissingQualifier() {
        assertThrows(IOException.class, () ->
                strategy.download(new PackageURL("pkg:generic/name@1.0"), Path.of("."))
        );
    }
}
