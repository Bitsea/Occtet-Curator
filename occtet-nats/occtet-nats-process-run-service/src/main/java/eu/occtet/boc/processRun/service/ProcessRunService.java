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


package eu.occtet.boc.processRun.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.squareup.okhttp.*;
import eu.occtet.boc.dao.OrtIssueRepository;
import eu.occtet.boc.dao.OrtViolationRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.OrtIssue;
import eu.occtet.boc.entity.OrtViolation;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.boc.processRun.factory.OrtIssueFactory;
import eu.occtet.boc.processRun.factory.OrtViolationFactory;
import org.apache.commons.codec.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.ProductsApi;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProcessRunService {

    private static final Logger log = LogManager.getLogger(ProcessRunService.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private OrtIssueFactory ortIssueFactory;

    @Autowired
    private OrtIssueRepository ortIssueRepository;

    @Autowired
    private OrtViolationFactory ortViolationFactory;

    @Autowired
    private OrtViolationRepository ortViolationRepository;

    private String clientId="ort-server";
    private String tokenUrl="http://localhost:8081/realms/master/protocol/openid-connect/token";
    private String username = "ort-admin";
    private String password = "password";


    public boolean process(ORTProcessWorkData workData) throws Exception {
        return fetchRun(workData.getRunId());
    }

    public boolean fetchRun(long runId) throws IOException, InterruptedException, ApiException {
        log.debug("Start processing run with id {}", runId);

        OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
        AuthService authService = new AuthService(tokenUrl);

        TokenResponse tokenResponse = authService.requestToken(clientId,username,password,"offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        RunsApi runsApi = new RunsApi(apiClient);

        ApiResponse<java.io.File> response = runsApi.getRunReportWithHttpInfo(runId, "bom.spdx.yml");
        log.debug("Requested report for run {} from ort server", runId);
        java.io.File object= response.getData();
        ObjectMapper objectMapper = new ObjectMapper();
        String fileString = Files.toString(object, Charsets.UTF_8);
        log.debug("Report for run {}: {}", runId, fileString);


        OrtRun run= runsApi.getRun(runId);
        Long productId= run.getProductId();
        ProductsApi productsApi = new ProductsApi(apiClient);
        Product product= productsApi.getProduct(productId);
        Project project = projectRepository.findByProjectName(product.getName()).getFirst();
        log.debug("Processing run {} for project {}", runId, project.getProjectName());

        handleViolations(runsApi, runId, project);

        handleIssues(runsApi, runId, project);


        //TODO delete Run at the end
        //runsApi.deleteRun(runId);
        return true;

    }

    private void handleViolations(RunsApi runsApi, Long runId, Project project) throws ApiException {
        PagedResponseRuleViolation pagedResponseRuleViolation= runsApi.getRunRuleViolations(runId, null, null, null, null);
        List<RuleViolation> ruleViolations= pagedResponseRuleViolation.getData();
        log.debug("Handle violations, found {} violations for run {}", ruleViolations.size(), runId);

        List<OrtViolation> toSaveViolations= new ArrayList<>();
        for(RuleViolation rV: ruleViolations){
            toSaveViolations.add(ortViolationFactory.createOrtViolation(rV.getMessage(), rV.getRule(),
                    rV.getSeverity().getValue(), rV.getPurl(), rV.getHowToFix(), rV.getLicense(), rV.getLicenseSource(), project));
        }
        if (!toSaveViolations.isEmpty()) {
            ortViolationRepository.saveAll(toSaveViolations);
            ortViolationRepository.flush();
        }

    }

    private void handleIssues(RunsApi runsApi, Long runId, Project project) throws ApiException {
        PagedResponseIssue pagedResponseIssue= runsApi.getRunIssues(runId, null, null, null, null);
        List<Issue> issues= pagedResponseIssue.getData();
        log.debug("Handle issues, found {} issues for run {}", issues.size(), runId);

        List<OrtIssue> toSaveIssues = new ArrayList<>();
        for(Issue issue: issues){

            toSaveIssues.add(ortIssueFactory.createOrtIssue(issue.getIdentifier().getName(), issue.getSeverity().getValue(),
                    issue.getPurl(), issue.getAffectedPath(), issue.getMessage(), issue.getSource(),
                    issue.getResolutions(), issue.getTimestamp(), issue.getWorker(), project));
        }

        if (!toSaveIssues.isEmpty()) {
            ortIssueRepository.saveAll(toSaveIssues);
            ortIssueRepository.flush();
        }

    }
}
