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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Strategy for downloading artifacts via direct HTTP/HTTPS connections.
 * <p>
 * This strategy handles:
 * <ul>
 * <li>Standard HTTP URLs (e.g., .zip, .tar.gz)</li>
 * <li>'Generic' PURLs that contains a 'download_url' qualifier,
 * @see <a href="https://github.com/package-url/purl-spec/blob/main/purl-types-index.json">PURL Types Index</a> and
 * @see <a href="https://github.com/package-url/purl-spec/blob/main/docs/known-qualifiers.md">PURL qualifiers</a>
 * </li>
 * </ul>
 */
@Component
@Order(2)
public class DirectHttpStrategy implements DownloadStrategy {

    private final Logger log = LogManager.getLogger(this.getClass());
    private final DownloadUtils downloadUtils; 

    private final String DOWNLOAD_URL_PURL_QUALIFIER = "download_url";
    private final String GENERIC = "generic";

    public DirectHttpStrategy(DownloadUtils downloadUtils) {
        this.downloadUtils = downloadUtils;
    }

    @Override
    public boolean canHandle(URL durl, String version) {
        if (durl == null) return false;
        String protocol = durl.getProtocol().toLowerCase();
        if (durl.getHost().contains("github.com")) {
            return false;
        }
        boolean canHandle = protocol.equals("http") || protocol.equals("https");
        log.info("Checking if {} can handle URL: {}. Result: {}", this.getClass().getSimpleName(), durl, canHandle);
        return canHandle;
    }

    @Override
    public boolean canHandle(PackageURL purl) {
        if (purl == null) return false;
        boolean canHandle = GENERIC.equalsIgnoreCase(purl.getType()) ||
                (purl.getQualifiers() != null && purl.getQualifiers().containsKey(DOWNLOAD_URL_PURL_QUALIFIER));
        log.debug("Checking if {} can handle PURL: {}. Result: {}", this.getClass().getSimpleName(), purl, canHandle);
        return canHandle;
    }

    @Override
    public Path download(URL durl, String version, Path targetDirectory) throws IOException {
        log.info("Executing {}.download for URL: {}", this.getClass().getSimpleName(), durl);
        return downloadUtils.downloadFile(durl.toString());
    }

    @Override
    public Path download(PackageURL purl, Path targetDirectory) throws IOException {
        log.info("Executing {}.download for PURL: {}", this.getClass().getSimpleName(), purl);

        String downloadUrl = null;
        if (purl.getQualifiers() != null){
            downloadUrl = purl.getQualifiers().get(DOWNLOAD_URL_PURL_QUALIFIER);
        }
        if (downloadUrl == null){
            throw new IOException(this.getClass().getSimpleName() + "cannot download Generic PURL: Missing " +
                    "'download_url' qualifier.");
        }

        return downloadUtils.downloadFile(downloadUrl);
    }
}
