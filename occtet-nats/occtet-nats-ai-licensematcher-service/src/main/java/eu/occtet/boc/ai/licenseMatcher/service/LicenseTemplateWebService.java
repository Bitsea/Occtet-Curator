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

package eu.occtet.boc.ai.licenseMatcher.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.occtet.boc.model.SPDXLicenseDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Service to fetch license information from spdx.org
 * either from web or from a default file in resources
 * used by LicenseTool for AI
 */
public class LicenseTemplateWebService {
    private static final Logger log = LoggerFactory.getLogger(LicenseTemplateWebService.class);

    public LicenseTemplateWebService() {}


    public SPDXLicenseDetails fetchDataFromWeb(String licenseId, String baseUrl) {
        log.debug("prepare to fetch license info from web");
        SPDXLicenseDetails details= null;
        String url= baseUrl+licenseId+".json";
        try {
            // download the license Text from the details Url
            WebClient client = WebClient.create(url);
            WebClient.RequestHeadersUriSpec<?> uriSpec = client.get();
            Mono<SPDXLicenseDetails> response = uriSpec.retrieve().bodyToMono(SPDXLicenseDetails.class);
            details = response.block();
            log.debug("control upload, licenseId: {}", details.licenseId());

            return details;
        }   catch (WebClientResponseException e){
            //Handling of 404 Not Found from GET https://spdx.org/licenses/<license>.json error.
            log.error("License information not Found from GET {} for the license: {} ",url, details.licenseId());
            return null;
        }


    }


    public SPDXLicenseDetails readDefaultLicenseInfos( String url) {
        try {
            log.debug("read URL {}", url);
            URL detailUrl = new URL(url);
            return readLicenseInfos(detailUrl.openStream());
        } catch (IOException e) {
            log.warn("could not read default license infos", e);
            return null;
        }
    }

    public SPDXLicenseDetails readLicenseInfos(InputStream inputStream) {
        try{

            InputStreamReader br = new InputStreamReader(inputStream);
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            SPDXLicenseDetails spdxLicenseDetails = gson.fromJson(br, SPDXLicenseDetails.class);

            log.info("processed '{}' license", spdxLicenseDetails.licenseId());
            return spdxLicenseDetails;
        } catch (Exception e) {
            log.error("licenses file could not be processed ", e);
            return null;
        }
    }
}
