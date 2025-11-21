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

package eu.occtet.bocfrontend.controller;

import eu.occtet.bocfrontend.scanner.FlexeraReportScanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;


@RestController
public class FlexeraReportController {
    private static final Logger log = LogManager.getLogger(FlexeraReportScanner.class);


    private final static String BACKEND_API = "http://localhost:8080";

    public String startFlexeraReportWorkflow(String inventoryItemName, String projectName) {

        String apiUrl = BACKEND_API+"/fossreport/"+inventoryItemName+"/"+projectName;
        log.debug("Calling Flexera Report API at URL: {}", apiUrl);

        RestClient restClient = RestClient.create();

        return restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(String.class);


    }

}
