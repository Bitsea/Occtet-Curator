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

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class DownloadUtils {

    private final Logger log = LogManager.getLogger(this.getClass());
    private final RestClient genericClient;

    static final String DEFAULT_FILE_NAME = "download_artifact";
    static final String TEMP_PREFIX = "occtet_dl_";
    static final String GIT_SSH_PREFIX = "git+ssh://";
    static final String SSH_PREFIX = "ssh://";
    static final String GIT_HTTPS_PREFIX = "git+https://";
    static final String GIT_HTTP_PREFIX = "git+http://";
    static final String HTTPS_PROTOCOL = "https://";

    public DownloadUtils(@Qualifier("genericRestClient") RestClient genericClient) {
        this.genericClient = genericClient;
    }

    /**
     * Standard download (tries to guess filename from URL).
     */
    public Path downloadFile(String url) throws IOException {
        return downloadFile(url, null);
    }

    /**
     * Download with forced extension (Critical for GitHub Zipballs).
     * @param forcedExtension e.g., ".zip" or ".tar.gz"
     */
    public Path downloadFile(String url, String forcedExtension) throws IOException {
        String safeUrl = normalizeUrl(url);
        String fileName = FilenameUtils.getName(safeUrl);

        if (fileName == null || !fileName.contains(".")) {
            fileName = DEFAULT_FILE_NAME;
        }

        if (forcedExtension != null && !fileName.toLowerCase().endsWith(forcedExtension.toLowerCase())) {
            fileName += forcedExtension;
        }

        Path tempFile = Files.createTempFile(TEMP_PREFIX, "_" + fileName);
        log.debug("Streaming download from '{}' to '{}'", safeUrl, tempFile);

        try {
            return genericClient.get()
                    .uri(safeUrl)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw new IOException("Download failed with HTTP " + response.getStatusCode() + " for " + safeUrl);
                        }
                        try (InputStream in = response.getBody()) {
                            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return tempFile;
                    });
        } catch (Exception e) {
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) { }
            throw new IOException("Failed to download from " + safeUrl, e);
        }
    }

    /**
     * Normalizes Git/SSH URLs to standard HTTPS URLs.
     */
    public String normalizeUrl(String rawURL) {
        if (rawURL == null) return null;
        String url = rawURL.trim();
        if (url.startsWith(GIT_SSH_PREFIX)) return url.replace(GIT_SSH_PREFIX, HTTPS_PROTOCOL);
        if (url.startsWith(SSH_PREFIX)) return url.replace(SSH_PREFIX, HTTPS_PROTOCOL);
        if (url.startsWith(GIT_HTTPS_PREFIX)) return url.substring(4);
        if (url.startsWith(GIT_HTTP_PREFIX)) return url.substring(4);
        return url;
    }

}
