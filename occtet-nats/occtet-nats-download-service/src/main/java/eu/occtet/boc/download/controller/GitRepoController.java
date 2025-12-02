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


@RestController
public class GitRepoController {

    private final static String GIT_API = "https://api.github.com/repos/";

    private final static int CORRECT_STATUSCODE = 200;

    private static final Logger log = LoggerFactory.getLogger(GitRepoController.class);

    public String getGitRepository(String owner, String repo, String version) throws IOException, InterruptedException {

        String url;
        HttpResponse<InputStream> response;
        StringBuilder apiUrl = new StringBuilder();
        apiUrl.append(GIT_API).append(owner).append("/").append(repo).append("/zipball");

        if(version != null){
            char firstCharacter = version.charAt(0);
            if(firstCharacter == 'v' || firstCharacter == 'V'){
                apiUrl.append("/").append(version);
                log.info("API-Url: {}",apiUrl);
                response = getHttpResponse(apiUrl.toString());

                log.info("Response: {}",response.statusCode());
                if(response.statusCode() == CORRECT_STATUSCODE){
                    url = response.uri().toString();
                    return url;
                }else{
                    int index = apiUrl.lastIndexOf("/");
                    apiUrl.deleteCharAt(index+1);
                    response = getHttpResponse(apiUrl.toString());
                    log.info("Modified api-url: {}",apiUrl);

                    log.info("Response: {}",response.statusCode());
                    if(response.statusCode() == CORRECT_STATUSCODE){
                        url = response.uri().toString();
                        return url;
                    }else{
                        return "";
                    }
                }
            }else{
                apiUrl.append("/").append(version);
                log.info("API-Url: {}",apiUrl);
                response = getHttpResponse(apiUrl.toString());

                log.info("Response: {}",response.statusCode());
                if(response.statusCode() == CORRECT_STATUSCODE){
                    url = response.uri().toString();
                    return url;
                }else{
                    int index = apiUrl.lastIndexOf("/");
                    apiUrl.insert(index+1,"v");
                    response = getHttpResponse(apiUrl.toString());
                    log.info("Modified api-url: {}",apiUrl);

                    log.info("Response: {}",response.statusCode());
                    if(response.statusCode() == CORRECT_STATUSCODE){
                        url = response.uri().toString();
                        return url;
                    }else{
                        return "";
                    }
                }
            }
        }
        return "";
    }

    private HttpResponse getHttpResponse(String apiUrl) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .header("Accept", "application/vnd.github+json").build();

        return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }
}
