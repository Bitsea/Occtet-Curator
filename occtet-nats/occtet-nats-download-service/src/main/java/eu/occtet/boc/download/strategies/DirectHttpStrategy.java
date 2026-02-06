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
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
public class DirectHttpStrategy implements DownloadStrategy {

    private final Logger log = LogManager.getLogger(this.getClass());

    private final String DOWNLOAD_URL_PURL_QUALIFIER = "download_url";
    private final String GENERIC = "generic";

    private final int CONNECTION_TIMEOUT = 10000;
    private final int READ_TIMEOUT = 180000;


    @Override
    public boolean canHandle(URL durl, String version) {
        if (durl == null) return false;
        String protocol = durl.getProtocol().toLowerCase();
        boolean canHandle = protocol.equals("http") || protocol.equals("https");
        log.debug("Checking if {} can handle URL: {}. Result: {}", this.getClass().getSimpleName(), durl, canHandle);
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
        return performDownload(durl.toString());
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

        return performDownload(downloadUrl);
    }

    private Path performDownload(String rawURL) throws IOException {
        log.debug("Performing download for URL: {}", rawURL);
        String effectiveURL = normalizeURL(rawURL);

        String fileName = FilenameUtils.getName(effectiveURL);
        if (fileName == null || fileName.isEmpty() || !fileName.contains(".")){
            fileName = "download_file.tmp";
        }
        Path tempFile = Files.createTempFile("occtet_dl_", "_" + fileName);
        log.debug("Downloading {} to temporary location {}", effectiveURL, tempFile);

        try {
            executeHttpReq(effectiveURL, tempFile);
            return tempFile;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ioe) {
                log.warn("Failed to delete temporary file {} after failed download: {}", tempFile, ioe.getMessage());
            }
            throw new IOException("Failed to download from " + effectiveURL, e);
        }
    }

    private void executeHttpReq(String urlString, Path destination) throws IOException {
        log.info("Executing HTTP request for URL: {}", urlString);
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String newURL = connection.getHeaderField("Location");
            log.debug("Redirect detected: {} -> {}", urlString, newURL);
            executeHttpReq(newURL, destination);
            return;
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP " + responseCode + " for URL: " + urlString);
        }
        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String normalizeURL(String rawURL){
        String url = rawURL.trim();
        if (url.startsWith("git+ssh://")) {
            log.info("Detected git+SSH URL:{}, replacing with HTTPS", url);
            return url.replace("git+ssh://", "https://");
        } else if (url.startsWith("ssh://")) {
            log.info("Detected SSH URL:{}, replacing with HTTPS", url);
            return url.replace("ssh://", "https://");
        } else if (url.startsWith("git+https://")) {
            log.info("Detected git+HTTPS URL:{}, replacing with HTTPS", url);
            return url.substring(4);
        } else if (url.startsWith("git+http://")) {
            log.info("Detected git+HTTP URL:{}, replacing with HTTP", url);
            return url.substring(4);
        }
        return url;
    }
}
