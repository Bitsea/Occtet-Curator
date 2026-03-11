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

package eu.occtet.boc.processRun.factory;

import eu.occtet.boc.dao.OrtIssueRepository;
import eu.occtet.boc.entity.OrtIssue;
import eu.occtet.boc.entity.Project;
import org.openapitools.client.model.IssueResolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrtIssueFactory {

    @Autowired
    private OrtIssueRepository ortIssueRepository;


    public OrtIssue createOrtIssue(String identifier, String severity, String purl, String affectedPath, String message, String source,
                               List<IssueResolution> resolutions, String timestamp, String worker, Project project) {
        List<String> resolutionDescriptions =new ArrayList<>();
        if(resolutions!= null) {
             resolutionDescriptions=resolutions.stream()
                    .map(IssueResolution::getMessage)
                    .toList();
        }
        String ident= "";
        if(identifier!= null) ident= identifier;
        OrtIssue ortIssue = new OrtIssue(ident, severity, purl, affectedPath, message, source,
                timestamp, worker, project, false);
        ortIssue.setResolution(resolutionDescriptions);
        return ortIssue;

    }


}
