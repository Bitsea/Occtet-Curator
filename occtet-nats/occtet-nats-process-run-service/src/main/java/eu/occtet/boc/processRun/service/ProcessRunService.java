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
import eu.occtet.boc.processRun.factory.ProjectFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.OrganizationsApi;
import org.openapitools.client.api.ProductsApi;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.*;
import org.openapitools.client.model.Organization;
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
    private ProjectFactory projectFactory;

    @Value("${https.cacert.path}")
    private String cacertPath;

    private final ConfigOrtProperties ortProperties;

    public ProcessRunService(ConfigOrtProperties ortProperties) {
        this.ortProperties = ortProperties;
    }

    public boolean process(ORTProcessWorkData workData) throws Exception {
        log.info("Processing ORTProcessWorkData for run ID: {}", workData.getRunId());
        return fetchRun(workData.getRunId());
    }

    public boolean fetchRun(long runId) throws IOException, InterruptedException, ApiException {
        log.info("Start processing ORT run with ID: {}", runId);

        OrtClientService ortClientService = new OrtClientService(ortProperties.baseUrl(), cacertPath,
                ortProperties.tokenUrl(), ortProperties.clientId());
        AuthService authService = new AuthService(ortProperties.tokenUrl(), cacertPath, ortProperties.clientSecret());

        log.debug("Requesting token from auth service for client ID: {}", ortProperties.clientId());
        TokenResponse tokenResponse = authService.requestToken(ortProperties.clientId(), ortProperties.username(),
                ortProperties.password(), "openid");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        RunsApi runsApi = new RunsApi(apiClient);

        log.debug("Fetching run details from ORT server for run ID: {}", runId);
        OrtRun run = runsApi.getRun(runId);
        Long productId = run.getProductId();
        ProductsApi productsApi = new ProductsApi(apiClient);
        Product product = productsApi.getProduct(productId);
        OrganizationsApi organizationsApi = new OrganizationsApi(apiClient);
        Organization organization = organizationsApi.getOrganization(product.getOrganizationId());

        log.info("Retrieved ORT Run {}: status='{}', product='{}' (ID: {}), organization='{}' (ID: {})",
                runId, run.getStatus(), product.getName(), productId, organization.getName(),
                product.getOrganizationId());

        Project project = null;
        List<Project> projects = projectRepository.findByProjectName(product.getName());
        if (projects.isEmpty()) {
            log.info("No existing project found in database for product '{}'. Creating new project...",
                    product.getName());
            project = projectFactory.createProject(product.getName(), organization.getName(), "1.0");
        } else {
            project = projects.getFirst();
            log.info("Found existing project in database: '{}' (ID: {})", project.getProjectName(), project.getId());
        }
        log.info("Processing run {} for project '{}' (ID: {})", runId, project.getProjectName(), project.getId());

        // process violations and issues
        handleViolations(runsApi, runId, project);
        handleIssues(runsApi, runId, project);

        return fetchAndDispatchSbom(runsApi, run, project.getId());
    }

    private boolean fetchAndDispatchSbom(RunsApi runsApi, OrtRun run, Long projectId) {
        long runId = run.getId();
        List<String> reportFilenames = new ArrayList<>();
        if (run.getJobs() != null && run.getJobs().getReporter() != null) {
            ReporterJob reporterJob = run.getJobs().getReporter();
            log.info("ORT Reporter job status for run {}: {}", runId, reporterJob.getStatus());
            if (reporterJob.getReportFilenames() != null) {
                reportFilenames.addAll(reporterJob.getReportFilenames());
            }
        }

        log.info("Available report filenames reported by ORT for run {}: {}", runId, reportFilenames);

        // 1. Try to find an SPDX file dynamically from reported filenames
        Optional<String> spdxFilename = reportFilenames.stream()
                .filter(name -> name.toLowerCase().contains("spdx"))
                .findFirst();

        String spdxToFetch = spdxFilename.orElse("bom.spdx.json");
        try {
            log.info("Attempting to fetch SPDX report ('{}') for run ID: {}", spdxToFetch, runId);
            ApiResponse<java.io.File> response = runsApi.getRunReportWithHttpInfo(runId, spdxToFetch);
            log.info(
                    "SPDX report ('{}') loaded successfully for run ID: {}. Dispatching to SPDX service for project ID: {}",
                    spdxToFetch, runId, projectId);
            boolean sent = answerService.sendToSpdxService(response.getData(), projectId, false, false);
            log.info("SPDX report dispatch result for run ID {}: {}", runId, sent);
            if (sent)
                return true;
        } catch (ApiException e) {
            log.warn("Could not fetch SPDX report '{}' for run ID {} (HTTP status: {}, message: {})", spdxToFetch,
                    runId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching SPDX report '{}' for run ID {}: {}", spdxToFetch, runId,
                    e.getMessage(), e);
        }

        // 2. Fallback: Try to find a CycloneDX file dynamically from reported filenames
        // or default candidate names
        Optional<String> cycloneDxFilename = reportFilenames.stream()
                .filter(name -> name.toLowerCase().contains("cyclonedx"))
                .findFirst();

        String cycloneToFetch = cycloneDxFilename.orElse("bom.cyclonedx.json");

        try {
            log.info("Attempting to fetch CycloneDX report ('{}') for run ID: {}", cFilename, runId);
            ApiResponse<java.io.File> response = runsApi.getRunReportWithHttpInfo(runId, cycloneToFetch);
            log.info(
                    "CycloneDX report ('{}') loaded successfully for run ID: {}. Dispatching to CycloneDX service for project ID: {}",
                    cFilename, runId, projectId);
            boolean sent = answerService.sendToCycloneDxService(response.getData(), projectId, false, false);
            log.info("CycloneDX report dispatch result for run ID {}: {}", runId, sent);
            if (sent)
                return true;
        } catch (ApiException e) {
            log.warn("Could not fetch CycloneDX report '{}' for run ID {} (HTTP status: {}, message: {})", cFilename,
                    runId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching CycloneDX report '{}' for run ID {}: {}", cFilename, runId,
                    e.getMessage(), e);
        }

        // 3. Fallback: Try ANY other report files reported by ORT if available
        for (String otherFilename : reportFilenames) {
            if (otherFilename.equalsIgnoreCase(spdxToFetch) || cycloneCandidates.contains(otherFilename)) {
                continue;
            }
            try {
                log.info("Attempting to fetch generic report ('{}') for run ID: {}", otherFilename, runId);
                ApiResponse<java.io.File> response = runsApi.getRunReportWithHttpInfo(runId, otherFilename);
                if (otherFilename.toLowerCase().contains("spdx") || otherFilename.toLowerCase().endsWith(".json")) {
                    log.info("Dispatching generic report '{}' to SPDX service for project ID: {}", otherFilename,
                            projectId);
                    return answerService.sendToSpdxService(response.getData(), projectId, false, false);
                } else {
                    log.info("Dispatching generic report '{}' to CycloneDX service for project ID: {}", otherFilename,
                            projectId);
                    return answerService.sendToCycloneDxService(response.getData(), projectId, false, false);
                }
            } catch (ApiException e) {
                log.warn("Could not fetch generic report '{}' for run ID {} (HTTP status: {}, message: {})",
                        otherFilename, runId, e.getCode(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error fetching generic report '{}' for run ID {}: {}", otherFilename, runId,
                        e.getMessage(), e);
            }
        }

        log.warn("No usable SBOM report could be resolved or fetched for ORT run ID: {} (Project ID: {})", runId,
                projectId);
        return false;
    }

    private void handleViolations(RunsApi runsApi, Long runId, Project project) throws ApiException {
        log.debug("Fetching rule violations from ORT for run ID: {}", runId);
        PagedResponseRuleViolation pagedResponseRuleViolation = runsApi.getRunRuleViolations(runId, null, null, null,
                null, null, null, null, null);
        List<RuleViolation> ruleViolations = pagedResponseRuleViolation.getData();
        log.info("Found {} rule violation(s) for run ID: {}", ruleViolations.size(), runId);

        List<OrtViolation> toSaveViolations = new ArrayList<>();
        for (RuleViolation rV : ruleViolations) {
            // workaround for bug in ort-server where licensesource is not set
            if (rV.getLicenseSource() == null)
                rV.setLicenseSource(LicenseSource.CONCLUDED);
            OrtViolation ortVio = ortViolationFactory.createOrtViolation(rV.getMessage(), rV.getRule(),
                    rV.getSeverity().getValue(), rV.getPurl(), rV.getHowToFix(), rV.getLicense(), rV.getLicenseSource(),
                    project);
            toSaveViolations.add(ortVio);
        }
        if (!toSaveViolations.isEmpty()) {
            ortViolationRepository.saveAll(toSaveViolations);
            ortViolationRepository.flush();
            log.info("Persisted {} rule violation(s) for project '{}' (ID: {})", toSaveViolations.size(),
                    project.getProjectName(), project.getId());
        }
    }

    private void handleIssues(RunsApi runsApi, Long runId, Project project) throws ApiException {
        log.debug("Fetching issues from ORT for run ID: {}", runId);
        PagedResponseIssue pagedResponseIssue = runsApi.getRunIssues(runId, null, null, null, null, null, null, null);
        List<Issue> issues = pagedResponseIssue.getData();
        log.info("Found {} issue(s) for run ID: {}", issues.size(), runId);

        List<OrtIssue> toSaveIssues = new ArrayList<>();
        for (Issue issue : issues) {
            OrtIssue ortIssue = ortIssueFactory.createOrtIssue(issue.getIdentifier().getName(),
                    issue.getSeverity().getValue(),
                    issue.getPurl(), issue.getAffectedPath(), issue.getMessage(), issue.getSource(),
                    issue.getResolutions(), issue.getTimestamp(), issue.getWorker(), project);
            toSaveIssues.add(ortIssue);
        }

        if (!toSaveIssues.isEmpty()) {
            ortIssueRepository.saveAll(toSaveIssues);
            ortIssueRepository.flush();
            log.info("Persisted {} issue(s) for project '{}' (ID: {})", toSaveIssues.size(), project.getProjectName(),
                    project.getId());
        }
    }
}
