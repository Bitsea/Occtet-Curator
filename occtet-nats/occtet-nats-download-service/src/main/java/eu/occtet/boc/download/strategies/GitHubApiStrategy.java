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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.packageurl.PackageURL;
import eu.occtet.boc.download.utils.DownloadUtils;
import eu.occtet.boc.download.utils.VersionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Strategy for identifying and downloading artifacts directly from GitHub repositories.
 * <p>
 * This strategy interacts with the GitHub API to resolve semantic version strings (e.g., "1.2")
 * into concrete Git tags (e.g., "v1.2.0-release") and downloads the associated source archive.
 * </p>
 */
@Component
@Order(1)
public class GitHubApiStrategy implements DownloadStrategy{

    private final Logger log = LogManager.getLogger(this.getClass());
    private final RestClient gitHubClient;
    private final DownloadUtils downloadUtils;
    private final VersionUtils versionUtils;

    private static final String GITHUB_HOST = "github.com";
    private static final String PURL_TYPE_GITHUB = "github";
    private final int TAGS_PER_PAGE = 100;

    public GitHubApiStrategy(@Qualifier("gitHubRestClient") RestClient gitHubClient,
                             DownloadUtils downloadUtils,
                             VersionUtils versionUtils) {
        this.gitHubClient = gitHubClient;
        this.downloadUtils = downloadUtils;
        this.versionUtils = versionUtils;
    }

    /**
     * Determines if the provided URL points to a GitHub repo
     *
     * @param durl      the URL to check
     * @param version   the version string
     * @return true if the URL host is 'github.com' otherwise false
     */
    @Override
    public boolean canHandle(URL durl, String version) {
        if (durl == null) return false;
        boolean isGitHub = durl.getHost().equalsIgnoreCase(GITHUB_HOST);
        log.info("Checking if {} can handle URL: {}. Result: {}", this.getClass().getSimpleName(), durl, isGitHub);
        return isGitHub;
    }

    /**
     * Determines if the provided PackageURL represents a GitHub package.
     *
     * @param purl The PackageURL to check.
     * @return {@code true} if the PURL type is 'github', otherwise {@code false}.
     */
    @Override
    public boolean canHandle(PackageURL purl) {
        if (purl == null) return false;
        boolean canHandle = PURL_TYPE_GITHUB.equalsIgnoreCase(purl.getType());
        log.debug("Checking if {} can handle PURL: {}. Result: {}", this.getClass().getSimpleName(), purl, canHandle);
        return canHandle;
    }

    @Override
    public Path download(URL durl, String version, Path targetDirectory) throws IOException {
        log.info("Executing {}.download for URL: {} @ {}", this.getClass().getSimpleName(), durl, version);
        RepoInfo repoInfo = parseGitHubUrl(durl.toString());
        String archiveUrl = resolveTagToArchiveUrl(repoInfo.owner, repoInfo.repo, version);
        return downloadUtils.downloadFile(archiveUrl, ".zip");
    }

    @Override
    public Path download(PackageURL purl, Path targetDirectory) throws IOException {
        log.info("Resolving GitHub PURL: {} with namespace: {}, name: {} and version: {}", purl,purl.getNamespace(), purl.getName(), purl.getVersion());
        if (purl.getNamespace() == null || purl.getName() == null) {
            throw new IOException("Invalid GitHub PURL: Namespace and Name required.");
        }
        String archiveUrl = resolveTagToArchiveUrl(purl.getNamespace(), purl.getName(), purl.getVersion());

        return downloadUtils.downloadFile(archiveUrl, ".zip");
    }

    /**
     * This method calculates the API URL
     */
    private String resolveTagToArchiveUrl(String owner, String repo, String version) throws IOException {
        String uri = String.format("/repos/%s/%s/tags?per_page="+TAGS_PER_PAGE, owner, repo);
        log.debug("Querying GitHub API: {}", uri);

        JsonNode tags = gitHubClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new IOException("GitHub Repository not found or private: " + owner + "/" + repo);
                }).body(JsonNode.class);
        if (tags == null) throw new IOException("Received empty response from GitHub API for " + owner + "/" + repo);
        for (JsonNode tag : tags) {
            String tagName = tag.get("name").asText();
            if (versionUtils.isMatch(tagName,version)){
                log.info("Resolved version '{}' to GitHub tag '{}'", version, tagName);
                if (tag.has("zipball_url")) {
                    return tag.get("zipball_url").asText();
                }
                return String.format("https://api.github.com/repos/%s/%s/zipball/%s", owner, repo, tagName);
            }
        }

        throw new IOException("Could not find GitHub tag for version: " + version);
    }

    private RepoInfo parseGitHubUrl(String rawURL) throws IOException {
        String cleanURL = rawURL.trim();

        // Remove "git+" prefix if present
        if (cleanURL.startsWith("git+")) cleanURL = cleanURL.substring(4);

        // Remove ".git" suffix if present (CRITICAL FIX)
        if (cleanURL.endsWith(".git")) cleanURL = cleanURL.substring(0, cleanURL.length() - 4);

        // Remove trailing slash
        if (cleanURL.endsWith("/")) cleanURL = cleanURL.substring(0, cleanURL.length() - 1);

        String[] parts = cleanURL.split("/");
        if (parts.length < 2)
            throw new IOException("Invalid GitHub URL: " + rawURL);

        String repo = parts[parts.length - 1];
        String owner = parts[parts.length - 2];
        return new RepoInfo(repo, owner);
    }

    private record RepoInfo(String repo, String owner) {}

}
