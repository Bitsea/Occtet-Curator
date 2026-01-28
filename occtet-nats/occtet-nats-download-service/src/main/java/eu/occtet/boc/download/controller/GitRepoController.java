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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import java.util.Locale;

/**
 * Controller responsible for resolving GitHub repository tags to download URLs.
 * <p>
 * Instead of guessing URLs, this controller queries the GitHub Tags API to retrieve
 * available versions and matches them against the requested version string using
 * fuzzy matching logic (e.g., handling 'v' prefixes, underscores, and release suffixes).
 */
@RestController
public class GitRepoController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String GITHUB_API_BASE = "https://api.github.com/repos";

    public GitRepoController() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String resolveGitToZipUrl(String repoOwner, String repoName, String version) throws IOException {
        // target api url "https://api.github.com/repos/%s/%s/tags?per_page=100&page=%d";
        String tagsUrl = String.format("%s/%s/%s/tags", GITHUB_API_BASE, repoOwner, repoName);
        log.debug("Fetching tags for {}/{} to find version {}", repoOwner, repoName, version);
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(tagsUrl + "?per_page=100", String.class);
            JsonNode tags = objectMapper.readTree(response.getBody());

            for (JsonNode tag : tags) {
                String tagName = tag.get("name").asText();

                if (isMatch(tagName, version)) {
                    log.info("Found matching tag '{}' for version '{}'.", tagName, version);
                    if (tag.has("zipball_url")) {
                        return tag.get("zipball_url").asText();
                    }
                    // Fallback construction
                    return String.format("%s/%s/%s/zipball/%s", "https://api.github.com/repos", repoOwner, repoName, tagName);
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new IOException("Repository not found or private: " + repoOwner + "/" + repoName, e);
        }
        throw new IOException(String.format("GitRepoController returned 404 for %s/%s @ %s (No matching tag found)", repoOwner, repoName, version));
    }


    /**
     * Robustly determines if a tag matches a version string using aggressive normalization.
     * <p>
     * <b>Normalization Logic:</b>
     * <ol>
     * <li>Strips non-digit prefixes (e.g., "v1.2", "release-1.2" -> "1.2").</li>
     * <li>Unifies separators: Replaces underscores and hyphens with dots.</li>
     * <li><b>Alphanumeric Boundaries:</b> Inserts dots between digits and letters.
     * <ul>
     * <li>"1.2.0a1" becomes "1.2.0.a.1"</li>
     * <li>"1.0rc1" becomes "1.0.rc.1"</li>
     * </ul>
     * </li>
     * </ol>
     * This ensures that "1.2.0-a.1", "1.2.0a1", and "1.2.0.a.1" all match each other.
     *
     * @param tagName the tag name from git
     * @param version the requested version
     * @return true if the tag matches the version
     */
    private boolean isMatch(String tagName, String version) {
        if (tagName.equalsIgnoreCase(version)) return true;

        String normalizedTag = normalizeVersion(tagName);
        String normalizedVersion = normalizeVersion(version);

        // Check A: Exact match of normalized strings
        if (normalizedTag.equals(normalizedVersion)) return true;

        // Check B: Tag contains Version (e.g. Tag "v1.2.1" matches Request "1.2")
        if (isPrefixWithBoundary(normalizedTag, normalizedVersion)) return true;

        // Check C: Version contains Tag (e.g. Tag "14.2.0" matches Request "14.2.0-19")
        if (isPrefixWithBoundary(normalizedVersion, normalizedTag)) return true;

        return false;
    }

    /**
     * Converts a raw version string into a canonical dot-separated format.
     * <p>
     * Examples:
     * <ul>
     * <li>"v1.2-3" -> "1.2.3"</li>
     * <li>"1_5_18" -> "1.5.18"</li>
     * <li>"1.2.0alpha1" -> "1.2.0.alpha.1"</li>
     * </ul>
     */
    private String normalizeVersion(String input) {
        String s = input.toLowerCase(Locale.ROOT);

        s = input.replaceAll("^[^0-9]+", "");
        s = s.replaceAll("[_\\-]", ".");
        s = s.replaceAll("(?<=\\d)(?=[a-zA-Z])", ".");
        s = s.replaceAll("(?<=[a-zA-Z])(?=\\d)", ".");

        return s;
    }

    /**
     * Checks if 'text' starts with 'prefix', ensuring a clean boundary.
     * <p>
     * Prevents false positives like "1.50" matching "1.5".
     */
    private boolean isPrefixWithBoundary(String text, String prefix) {
        if (!text.startsWith(prefix)) return false;

        if (text.length() == prefix.length()) return true;

        char nextChar = text.charAt(prefix.length());
        return !Character.isDigit(nextChar);
    }
}