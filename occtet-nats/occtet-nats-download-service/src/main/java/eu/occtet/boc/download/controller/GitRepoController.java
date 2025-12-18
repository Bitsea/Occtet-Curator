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

package eu.occtet.boc.download.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Controller responsible for resolving GitHub repository tags to download URLs.
 * <p>
 * Instead of guessing URLs, this controller queries the GitHub Tags API to retrieve
 * available versions and matches them against the requested version string using
 * fuzzy matching logic (e.g., handling 'v' prefixes, underscores, and release suffixes).
 */
@RestController
public class GitRepoController {

    private final static String GIT_API_TAGS_TEMPLATE = "https://api.github.com/repos/%s/%s/tags?per_page=100&page=%d";
    private final static int SUCCESS_STATUS_CODE = 200;
    private final static int MAX_PAGES_TO_SCAN = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private static final Logger log = LoggerFactory.getLogger(GitRepoController.class);

    public GitRepoController() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Retrieves the download URL of a GitHub repository ZIP file for a specific version.
     * <p>
     * This method fetches the list of tags from the repository and iterates through them
     * to find a match for the provided version string. It handles common versioning
     * discrepancies such as 'v' prefixes (v1.0 vs 1.0) or different separators.
     *
     * @param owner   the owner or organization of the GitHub repository
     * @param repo    the name of the GitHub repository
     * @param version the version identifier to locate
     * @return the URL of the zipball for the matching tag, or an empty string if not found
     */
    public String getGitRepository(String owner, String repo, String version) throws IOException, InterruptedException {
        if (version == null || version.isEmpty()) {
            return "";
        }

        for (int page = 1; page <= MAX_PAGES_TO_SCAN; page++) {
            String apiUrl = String.format(GIT_API_TAGS_TEMPLATE, owner, repo, page);
            log.debug("Fetching tags for {}/{} (Page {})", owner, repo, page);

            HttpResponse<InputStream> response = getHttpResponse(apiUrl);

            if (response.statusCode() != SUCCESS_STATUS_CODE) {
                log.warn("Failed to fetch tags for {}/{} on page {}. Status: {}", owner, repo, page, response.statusCode());
                break;
            }

            try (InputStream body = response.body()) {
                JsonNode tagsArray = objectMapper.readTree(body);

                if (tagsArray.isEmpty()) {
                    log.debug("No more tags found on page {}. Stopping search.", page);
                    break;
                }

                // Iterate tags on this page
                for (JsonNode tagNode : tagsArray) {
                    String tagName = tagNode.get("name").asText();

                    if (matchesVersion(tagName, version, repo)) {
                        String zipUrl = tagNode.get("zipball_url").asText();
                        log.info("Found matching tag '{}' for version '{}'. URL: {}", tagName, version, zipUrl);
                        return zipUrl;
                    }
                }
            }
        }

        log.warn("No matching tag found for {}/{} version {} after scanning {} pages.", owner, repo, version, MAX_PAGES_TO_SCAN);
        return "";
    }

    /**
     * Determines if a Git tag matches the requested version string.
     * <p>
     * Implements heuristics to handle common versioning variations:
     * <ul>
     * <li>Exact match (e.g., "1.0" == "1.0")</li>
     * <li>'v' prefix (e.g., "v1.0" == "1.0")</li>
     * <li>Repo name prefix (e.g., "gson-2.8.0" == "2.8.0")</li>
     * <li>Underscore substitution (e.g., "GROOVY_2_4" == "2.4")</li>
     * <li>Release folder prefixes (e.g., "release/1.0" == "1.0")</li>
     * </ul>
     */
    private boolean matchesVersion(String tagName, String targetVersion, String repoName) {
        String cleanTag = tagName.toLowerCase();
        String cleanTarget = targetVersion.toLowerCase();
        String cleanRepo = repoName.toLowerCase();

        // 1. Exact Match (e.g. "1.0")
        if (cleanTag.equals(cleanTarget)) return true;

        // 2. 'v' prefix (e.g. "v1.0")
        if (cleanTag.equals("v" + cleanTarget)) return true;

        // 3. Repo-Prefix (e.g. "gson-2.8.0")
        if (cleanTag.equals(cleanRepo + "-" + cleanTarget)) return true;
        if (cleanTag.equals(cleanRepo + "-v" + cleanTarget)) return true;

        // 4. "Parent" naming convention (fixes Gson issue: "gson-parent-2.8.0")
        if (cleanTag.endsWith("-" + cleanTarget)) {
            if (cleanTag.endsWith("-" + cleanTarget) || cleanTag.endsWith("-v" + cleanTarget)) {
                return true;
            }
        }

        // 5. Underscore variation (e.g. "GROOVY_4_0_26")
        String underscoreTarget = cleanTarget.replace(".", "_");
        if (cleanTag.equals(underscoreTarget)) return true;
        if (cleanTag.equals("v_" + underscoreTarget)) return true;
        if (cleanTag.equals(cleanRepo + "_" + underscoreTarget)) return true;

        // 6. Release/Rel prefix
        if (cleanTag.equals("rel/" + cleanTarget)) return true;
        if (cleanTag.equals("release/" + cleanTarget)) return true;

        return false;
    }

    private HttpResponse<InputStream> getHttpResponse(String apiUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Occtet-Downloader")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }
}