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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
public class GitRepoController {

    private final static String GIT_API = "https://api.github.com/repos/";
    private final static int SUCCESS_STATUS_CODE = 200;
    private static final Logger log = LoggerFactory.getLogger(GitRepoController.class);

    /**
     * Retrieves the download URL of a GitHub repository ZIP file for a specific version or tag.
     * The method constructs possible tag candidates based on the provided version and
     * attempts to locate a valid tag in the GitHub repository. If a valid tag is found,
     * the corresponding download URL is returned.
     *
     * @param owner the owner or organization of the GitHub repository
     * @param repo the name of the GitHub repository
     * @param version the version or tag to be checked in the repository
     * @return the download URL of the repository ZIP file for*/
    public String getGitRepository(String owner, String repo, String version) throws IOException, InterruptedException {
        if (version == null || version.isEmpty()) {
            return "";
        }

        List<String> tagCandidates = new ArrayList<>();

        // Case 1: Exact Version
        tagCandidates.add(version);

        // Case 2: 'v' prefix logic
        String versionNoV = version;
        if (version.toLowerCase().startsWith("v")) {
            versionNoV = version.substring(1);
            tagCandidates.add(versionNoV);
        } else {
            tagCandidates.add("v" + version);
        }

        // Case 3: Repo-Prefix combinations (e.g. "awaitility-4.3.0")
        tagCandidates.add(repo + "-" + version);

        if (!version.equals(versionNoV)) {
            tagCandidates.add(repo + "-" + versionNoV);
        }
        if (!version.toLowerCase().startsWith("v")) {
            tagCandidates.add(repo + "-v" + version);
        }

        // Case 4:  Underscore variation (e.g. logback "v_1.5.18")
        if (version.toLowerCase().startsWith("v")) {
            tagCandidates.add(version.replace("v", "v_"));
        } else {
            tagCandidates.add("v_" + version);
        }

        // Case 5: Apache/Groovy Style (e.g. "GROOVY_4_0_26")
        // Logic: REPO (UPPER) + "_" + VERSION (dots -> underscores)
        String underscoreVersion = versionNoV.replace(".", "_");
        tagCandidates.add(repo.toUpperCase() + "_" + underscoreVersion);

        // Case 6: "rel/" prefix (Common in some older repos)
        tagCandidates.add("rel/" + version);
        tagCandidates.add("release/" + version);

        for (String tag : tagCandidates) {
            String url = constructZipUrl(owner, repo, tag);
            log.info("Checking API-Url: {}", url);

            HttpResponse<InputStream> response = getHttpResponse(url);

            if (response.statusCode() == SUCCESS_STATUS_CODE) {
                log.info("Found valid tag: {}", tag);
                return response.uri().toString();
            }
        }

        log.warn("No valid tag found for {}/{} version {}", owner, repo, version);
        return "";
    }

    private String constructZipUrl(String owner, String repo, String tag) {
        return GIT_API + owner + "/" + repo + "/zipball/" + tag;
    }

    private HttpResponse<InputStream> getHttpResponse(String apiUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .header("Accept", "application/vnd.github+json")
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }
}