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

package eu.occtet.boc.search.service;

import eu.occtet.boc.model.FileSearchServiceWorkData;
import eu.occtet.boc.model.PaginatedSearchResponse;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.RequestReplySubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class FileSearchRequestSubscriber extends RequestReplySubscriber<WorkTask, PaginatedSearchResponse> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String SEARCH_REQUEST_SUBJECT = ""; // TODO

    @Autowired
    public FileSearchService fileSearchService;

    @Override
    public String getSubject() {
        return SEARCH_REQUEST_SUBJECT;
    }

    @Override
    protected Class<WorkTask> getRequestClass() {
        return WorkTask.class;
    }

    @Override
    protected PaginatedSearchResponse handleRequest(WorkTask request) throws Exception {
        if (request.workData() instanceof FileSearchServiceWorkData searchData) {

            log.debug("Processing search for project: {}", searchData.getProjectId());

            return fileSearchService.searchPage(
                    searchData.getSearchText(),
                    searchData.getProjectId().toString(),
                    searchData.getMaxNumberOfFindings(),
                    searchData.getDirection(),
                    searchData.getPaginationToken()
            );
        } else {
            throw new IllegalArgumentException("WorkData of type " + request.workData().getClass().getName() + " is not supported");
        }
    }

    @Override
    protected PaginatedSearchResponse handleError(Exception e) {
        log.error("Search request failed: {}", e.getMessage());
        return new PaginatedSearchResponse(
                Collections.emptyList(), null, null, "Search request failed: " + e.getMessage()
        );
    }
}
