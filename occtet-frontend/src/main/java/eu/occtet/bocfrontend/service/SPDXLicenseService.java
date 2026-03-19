/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.factory.LicenseFactory;
import eu.occtet.bocfrontend.model.SPDXLicenseDetails;
import eu.occtet.bocfrontend.model.SPDXLicenseInfos;
import io.jmix.core.DataManager;
import io.jmix.core.security.Authenticated;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.reflections.Reflections.log;

@Service
public class SPDXLicenseService {

    @Autowired
    private LicenseFactory licenseFactory;


    @Authenticated
    public void readDefaultLicenseInfos() {
        try {

            URL url = new URL("https://raw.githubusercontent.com/spdx/license-list-data/main/json/licenses.json");
            log.info("Reading spdx/default license infos from {}", url);
            readLicenseInfos(url.openStream());
        } catch (IOException e) {
            log.warn("could not read default license infos", e);
        }
    }

    public void readLicenseInfos(InputStream inputStream) {
        try{

            InputStreamReader br = new InputStreamReader(inputStream);
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            SPDXLicenseInfos spdxLicenseInfos = gson.fromJson(br, SPDXLicenseInfos.class);

            List<License> licenses = spdxLicenseInfos.getLicenses();
            log.debug("Size of spdx licenses: {}",licenses.size());
            if(!licenses.isEmpty() && licenses != null){saveLicenseInfos(licenses);}

        } catch (Exception e) {
            log.error("licenses file could not be processed ", e);
        }
    }

    private void saveLicenseInfos(List<License> licenses){

        licenses.forEach(license -> {
            if(license.getLicenseType() == null){
                license.setLicenseType("");
            }
            if(license.getLicenseName() == null){
                license.setLicenseName("");
            }
            if(license.getDetailsUrl() == null){
                license.setDetailsUrl("");
            }else{
                downloadLicenseText(license,license.getDetailsUrl());
            }
            licenseFactory.create(license.getLicenseType(),license.getLicenseText(),
                    license.getLicenseName(), license.getDetailsUrl(), isSpdxLicense(license));
        });
    }

    private boolean isSpdxLicense(License license) {
        return license.getDetailsUrl().startsWith("https://spdx.org");
    }

    private void downloadLicenseText(License license, String url) {
        try {
            // download the license Text from the details Url
            WebClient client = WebClient.create(url);
            WebClient.RequestHeadersUriSpec<?> uriSpec = client.get();
            Mono<SPDXLicenseDetails> response = uriSpec.retrieve().bodyToMono(SPDXLicenseDetails.class);
            SPDXLicenseDetails details = response.block();
            if (details != null && !StringUtils.isEmpty(details.getLicenseText())) {

                if (StringUtils.isEmpty(license.getLicenseText()) || !license.getLicenseText().equals(details.getLicenseText())) {
                    license.setLicenseText(details.getLicenseText());
                    log.debug("downloaded license text for {} ", license.getLicenseType());
                }
            }
        } catch (WebClientResponseException e) {
            //Handling of 404 Not Found from GET https://spdx.org/licenses/<license>.json error.
            log.error("License information not Found from GET {} for the license: {} ", url, license.getLicenseType());
        }
    }
}
