/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.download.strategies;

import com.github.packageurl.PackageURL;
import eu.occtet.boc.download.utils.DownloadUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy for downloading source archives from Maven Central.
 * <p>
 * Handles:
 * <ul>
 * <li>Maven PURLs of type {@code pkg:maven/groupId/artifactId@version}</li>
 * <li>Name-based lookups where the name follows the pattern
 * {@code groupId:artifactId}</li>
 * </ul>
 * The download targets the {@code -sources.jar} artifact on Maven Central.
 * Falls back to the plain {@code .jar} if no sources jar is found.
 */
@Component
@Order(4)
public class MavenStrategy implements DownloadStrategy {

    private final Logger log = LogManager.getLogger(this.getClass());
    private final DownloadUtils downloadUtils;

    private static final String PURL_TYPE_MAVEN = "maven";
    private static final String MAVEN_CENTRAL_BASE = "https://repo1.maven.org/maven2";

    public MavenStrategy(DownloadUtils downloadUtils) {
        this.downloadUtils = downloadUtils;
    }

    /**
     * Handles Maven PURLs of the form {@code pkg:maven/groupId/artifactId@version}.
     */
    @Override
    public boolean canHandle(PackageURL purl) {
        if (purl == null)
            return false;
        boolean canHandle = PURL_TYPE_MAVEN.equalsIgnoreCase(purl.getType());
        log.debug("Checking if {} can handle PURL: {}. Result: {}", this.getClass().getSimpleName(), purl, canHandle);
        return canHandle;
    }

    /**
     * Handles name-based lookups where the name follows the Maven convention
     * {@code groupId:artifactId}.
     */
    @Override
    public boolean canHandle(String pckName, String version) {
        if (pckName == null || version == null)
            return false;
        boolean canHandle = pckName.contains(":");
        log.debug("Checking if {} can handle name '{}'. Result: {}", this.getClass().getSimpleName(), pckName,
                canHandle);
        return canHandle;
    }

    @Override
    public Path download(PackageURL purl, Path targetDirectory) throws IOException {
        log.info("Executing {}.download for PURL: {}", this.getClass().getSimpleName(), purl);

        String groupId = purl.getNamespace();
        String artifactId = purl.getName();
        String version = purl.getVersion();

        if (groupId == null || artifactId == null || version == null) {
            throw new IOException("Invalid Maven PURL – groupId, artifactId and version are required: " + purl);
        }

        return downloadFromMavenCentral(groupId, artifactId, version);
    }

    @Override
    public Path download(String name, String version, Path targetDirectory) throws IOException {
        log.info("Executing {}.download for name '{}' @ '{}'", this.getClass().getSimpleName(), name, version);

        String[] parts = name.split(":", 2);
        if (parts.length != 2) {
            throw new IOException("Invalid Maven name format – expected 'groupId:artifactId', got: " + name);
        }

        return downloadFromMavenCentral(parts[0], parts[1], version);
    }

    // -------------------------------------------------------------------------

    private Path downloadFromMavenCentral(String groupId, String artifactId, String version) throws IOException {
        String groupPath = groupId.replace('.', '/');
        String sourcesUrl = String.format("%s/%s/%s/%s/%s-%s-sources.jar",
                MAVEN_CENTRAL_BASE, groupPath, artifactId, version, artifactId, version);

        log.debug("Trying Maven sources JAR: {}", sourcesUrl);
        try {
            return downloadUtils.downloadFile(sourcesUrl, ".jar");
        } catch (IOException e) {
            log.warn("Sources JAR not found for {}:{}:{}, falling back to plain JAR – {}",
                    groupId, artifactId, version, e.getMessage());
        }

        String jarUrl = String.format("%s/%s/%s/%s/%s-%s.jar",
                MAVEN_CENTRAL_BASE, groupPath, artifactId, version, artifactId, version);

        log.debug("Trying Maven plain JAR: {}", jarUrl);
        return downloadUtils.downloadFile(jarUrl, ".jar");
    }
}
