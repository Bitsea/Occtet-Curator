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
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.OrtIssueRepository;
import eu.occtet.boc.dao.OrtViolationRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.boc.processRun.config.ConfigOrtProperties;
import eu.occtet.boc.processRun.factory.OrtIssueFactory;
import eu.occtet.boc.processRun.factory.OrtViolationFactory;
import org.apache.commons.codec.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.ProductsApi;
import org.openapitools.client.api.RepositoriesApi;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private AnswerService answerService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    private final ConfigOrtProperties ortProperties;

    public ProcessRunService(ConfigOrtProperties ortProperties) {
        this.ortProperties = ortProperties;
    }


    public boolean process(ORTProcessWorkData workData) throws Exception {
        return fetchRun(workData.getRunId());
    }

    public boolean fetchRun(long runId) throws IOException, InterruptedException, ApiException {
        log.debug("Start processing run with id {}", runId);

        OrtClientService ortClientService = new OrtClientService(ortProperties.baseUrl());
        AuthService authService = new AuthService(ortProperties.tokenUrl());

        TokenResponse tokenResponse = authService.requestToken(ortProperties.clientId(), ortProperties.username(), ortProperties.password(), "offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        RunsApi runsApi = new RunsApi(apiClient);

        ApiResponse<java.io.File> response = runsApi.getRunReportWithHttpInfo(runId, "bom.spdx.json");
        log.debug("Requested report for run {} from ort server", runId);
        java.io.File spdxSbom= response.getData();


        OrtRun run= runsApi.getRun(runId);
        Long productId= run.getProductId();
        ProductsApi productsApi = new ProductsApi(apiClient);
        Product product= productsApi.getProduct(productId);
        Project project = projectRepository.findByProjectName(product.getName()).getFirst();
        log.debug("Processing run {} for project {}", runId, project.getProjectName());


        //handle violations and issues for display in UI and further processing
        handleViolations(runsApi, runId, project);
        handleIssues(runsApi, runId, project);

        //send sbom to spdx service for further processing, AI is for now not triggered -> false, false
        answerService.sendToSpdxService(spdxSbom,project.getId(), false, false);

        //delete Run at the end
        runsApi.deleteRun(runId);
        return true;

    }

    private void handleViolations(RunsApi runsApi, Long runId, Project project) throws ApiException {
        PagedResponseRuleViolation pagedResponseRuleViolation= runsApi.getRunRuleViolations(runId, null, null, null, null);
        List<RuleViolation> ruleViolations= pagedResponseRuleViolation.getData();
        log.debug("Handle violations, found {} violations for run {}", ruleViolations.size(), runId);

        List<OrtViolation> toSaveViolations= new ArrayList<>();
        for(RuleViolation rV: ruleViolations){
            OrtViolation ortVio= ortViolationFactory.createOrtViolation(rV.getMessage(), rV.getRule(),
                    rV.getSeverity().getValue(), rV.getPurl(), rV.getHowToFix(), rV.getLicense(), rV.getLicenseSource(), project);
            if(ortVio.getPurl()!=null) {
                List<InventoryItem> inventoryItem = inventoryItemRepository.findByProjectIdAndSoftwareComponentPurl(project.getId(), ortVio.getPurl());
                ortVio.setInventoryItem(inventoryItem.getFirst());
            }
            toSaveViolations.add(ortVio);

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

            OrtIssue ortIssue= ortIssueFactory.createOrtIssue(issue.getIdentifier().getName(), issue.getSeverity().getValue(),
                    issue.getPurl(), issue.getAffectedPath(), issue.getMessage(), issue.getSource(),
                    issue.getResolutions(), issue.getTimestamp(), issue.getWorker(), project);
            if(ortIssue.getPurl()!=null) {
                List<InventoryItem> inventoryItems = inventoryItemRepository.findByProjectIdAndSoftwareComponentPurl(project.getId(), ortIssue.getPurl());
                ortIssue.setInventoryItem(inventoryItems.getFirst());
            }
            toSaveIssues.add(ortIssue);
        }

        if (!toSaveIssues.isEmpty()) {
            ortIssueRepository.saveAll(toSaveIssues);
            ortIssueRepository.flush();
        }

    }
}
