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

package eu.occtet.boc.ortrunstart.service;

import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.model.ORTStartRunWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.boc.ortrunstart.config.ConfigOrtProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.OrganizationsApi;
import org.openapitools.client.api.ProductsApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import eu.occtet.boc.entity.Project;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ORTRunStarterService {

    private static final Logger log = LogManager.getLogger(ORTRunStarterService.class);

    @Value("${https.cacert.path}")
    private String cacertPath;

    @Autowired
    private ProjectRepository projectRepository;

    private static final String GITHUB_TOKEN = "GITHUB-TOKEN";
    private static final String INFRASTRUCTURE_SERVICE_NAME = "RepositoryService";

    private final ConfigOrtProperties ortProperties;

    public ORTRunStarterService(ConfigOrtProperties ortProperties) {
        this.ortProperties = ortProperties;
    }

    public boolean process(ORTStartRunWorkData workData) throws Exception {
        return startOrtRun(workData.getProjectId(),workData.getRepositoryType());
    }

    boolean startOrtRun(long projectId, String repoType) throws IOException, InterruptedException, ApiException {

        log.debug("connecting with base: {} / and token: {}", ortProperties.baseUrl(), ortProperties.tokenUrl());

        Project project= projectRepository.findById(projectId).get();
        String orgaName= project.getOrganization().getOrganizationName();
        log.info("connection with ORT on {}, add. cacerts from {}", ortProperties.baseUrl(), cacertPath);
        OrtClientService ortClientService = new OrtClientService(ortProperties.baseUrl(), cacertPath, ortProperties.tokenUrl(), ortProperties.clientId());
        AuthService authService = new AuthService(ortProperties.tokenUrl(), cacertPath);
        log.info("authcall on keycloak with clientId {} username {} password {}", ortProperties.clientId(), ortProperties.username(), ortProperties.password().substring(0,2)+"..." );

        TokenResponse tokenResponse = authService.requestToken(ortProperties.clientId(), ortProperties.username(), ortProperties.password(), "offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        // how to access organizations api
        OrganizationsApi organizationsApi = new OrganizationsApi(apiClient);
        Organization orga= createOrganization(orgaName, organizationsApi);

        //check if product /project is existing
        ProductsApi productsApi = new ProductsApi(apiClient);
        Product product = createProduct(project, organizationsApi, orga);
        if (project.getGithubToken() != null) {
            createInfraStructureService(productsApi, product, project);
        }

        Repository repository= createRepository(productsApi, product, project.getRepositoryURL(), repoType, project.getProjectName());

        PostRepositoryRun postRepositoryRun= new PostRepositoryRun();
        postRepositoryRun.setRevision(project.getVersion());
        JobConfigurations jobConfigurations= createJobConfig();
        postRepositoryRun.setJobConfigs(jobConfigurations);
        postRepositoryRun.setRepositoryIds(List.of(repository.getId()));
        //is this how i start the run?
        List<OrtRun> runs = productsApi.postProductRuns(product.getId(), postRepositoryRun);
        runs.stream().forEach(run-> log.debug("Started run with id {} created at {} with productid {} repoId {} ", run.getId(),run.getCreatedAt(), run.getProductId(), run.getRepositoryId()));

        return true;

    }

    private void createInfraStructureService(ProductsApi productsApi, Product product, Project project) throws ApiException {
        PagedResponseInfrastructureService pagedResponseInfrastructureService = productsApi.getProductInfrastructureServices(product.getId(), null, null,null);
        List<InfrastructureService> dataInfra = pagedResponseInfrastructureService.getData();
        Optional<InfrastructureService> service = dataInfra.stream().filter(i -> i.getName().equals(INFRASTRUCTURE_SERVICE_NAME+project.getProjectName())).findFirst();

        if (dataInfra.isEmpty() || service.isEmpty()) {
            Secret passwordSecret = createGithubPasswordSecretForProduct(product, productsApi, project);
            Secret userSecret = createGithubUserSecretForProduct(product, productsApi, project);

            PostInfrastructureService postInfrastructureService = new PostInfrastructureService();
            postInfrastructureService.setName(INFRASTRUCTURE_SERVICE_NAME + project.getProjectName());
            postInfrastructureService.setUsernameSecretRef(userSecret.getName());
            postInfrastructureService.setUrl(project.getRepositoryURL());
            postInfrastructureService.addCredentialsTypesItem(CredentialsType.GIT_CREDENTIALS_FILE);
            postInfrastructureService.setPasswordSecretRef(passwordSecret.getName());
            log.debug("InfrastructureService with name {} created for product {}", postInfrastructureService.getName(), product.getName());

            productsApi.postProductInfrastructureService(product.getId(), postInfrastructureService);
        } else {
            log.debug("InfrastructureService {} already exists", INFRASTRUCTURE_SERVICE_NAME+project.getProjectName());
        }
    }

    private Organization createOrganization(String orgaName, OrganizationsApi organizationsApi) throws ApiException {
        try {
            //First check if orga is already existing
            PagedResponseOrganization organisation = organizationsApi.getOrganizations(null, null, null, orgaName);
            List<Organization> data = organisation.getData();
            Optional<Organization> organization = data.stream().filter(o -> o.getName().equals(orgaName)).findFirst();

            Organization orga = null;
            if (data.isEmpty() || organization.isEmpty()) {
                // nothing there? create an organization
                log.debug("Organization {} not found, creating it", orgaName);
                PostOrganization po = new PostOrganization();
                po.setName(orgaName);
                orga = organizationsApi.postOrganization(po);
            } else {
                log.debug("Organization {} found", orgaName);
                orga = organization.get();
            }
            return orga;
        }catch(Exception e){
            log.error("Error while cretating organization: {} with error: {}", orgaName,e.getMessage() );
            throw e;
        }

    }

    private Secret createGithubPasswordSecretForProduct(Product product, ProductsApi productsApi, Project project) throws ApiException {
        PagedResponseSecret pagedResponseSecret= productsApi.getProductSecrets(product.getId(), null, null, null);
        List<Secret> dataProd = pagedResponseSecret.getData();
        Optional<Secret> secretGit= dataProd.stream().filter(s -> s.getName().equalsIgnoreCase(project.getGithubUser()+GITHUB_TOKEN)).findFirst();
        Secret secret=null;
        if(dataProd.isEmpty() || secretGit.isEmpty()) {
            log.debug("Secret {} not found, creating it", project.getGithubUser()+GITHUB_TOKEN);
            PostSecret postSecret= new PostSecret().name(project.getGithubUser()+GITHUB_TOKEN);
            postSecret.setName(project.getGithubUser()+GITHUB_TOKEN);
            postSecret.setValue(project.getGithubToken());
            secret= productsApi.postProductSecret(product.getId(), postSecret);
        } else {
            log.debug("Secret {} found", secretGit.get().getName());
            secret= secretGit.get();
        }
        return secret;
    }

    private Secret createGithubUserSecretForProduct(Product product, ProductsApi productsApi, Project project) throws ApiException {
        PagedResponseSecret pagedResponseSecret= productsApi.getProductSecrets(product.getId(), null, null, null);
        List<Secret> dataProd = pagedResponseSecret.getData();
        Optional<Secret> secretGit= dataProd.stream().filter(s -> s.getName().equalsIgnoreCase(project.getGithubUser())).findFirst();
        Secret secret= null;
        if(dataProd.isEmpty() || secretGit.isEmpty()) {
            log.debug("Secret {} not found, creating it", project.getGithubUser());
            PostSecret postSecret= new PostSecret().name(project.getGithubUser());
            postSecret.setName(project.getGithubUser());
            postSecret.setValue(project.getGithubUser());
            secret= productsApi.postProductSecret(product.getId(), postSecret);
        } else {
            log.debug("Secret {} found", secretGit.get().getName());
            secret= secretGit.get();
        }
        return secret;
    }

    private Product createProduct(Project project, OrganizationsApi organizationsApi, Organization orga) throws ApiException {
        PagedResponseProduct pagedResponseProduct = organizationsApi.getOrganizationProducts(orga.getId(), null, null, null, project.getProjectName());
        List<Product> dataProd = pagedResponseProduct.getData();
        Optional<Product> prod= dataProd.stream().filter(p -> p.getName().equals(project.getProjectName())).findFirst();
        Product product= null;

        if(dataProd.isEmpty() || prod.isEmpty()) {
            log.debug("Product {} not found, creating it", project.getProjectName());
            PostProduct pro= new PostProduct().name(project.getProjectName());
            pro.setName(project.getProjectName());
            pro.setDescription("test");
            product= organizationsApi.postProduct(orga.getId(), pro);
        } else {
            log.debug("Product {} found", project.getProjectName());
            product= prod.get();
        }
        return product;
    }


    private Repository createRepository(ProductsApi productsApi, Product product, String repoURL, String repoType, String repoName) throws ApiException {
        //check if repository is already existing
        PagedResponseRepository pagedResponseRepository= productsApi.getProductRepositories(product.getId(), null, null, null, repoName);
        List<Repository> dataRepo = pagedResponseRepository.getData();
        Optional<Repository> repo= dataRepo.stream().filter(r -> r.getUrl().equals(repoURL)).findFirst();
        Repository repository= null;

        if(dataRepo.isEmpty()|| repo.isEmpty() ){
            log.debug("Repository {} not found, creating it", repoURL);
            PostRepository postRepository= new PostRepository();
            postRepository.setUrl(repoURL);
            RepositoryType repositoryType = RepositoryType.fromValue(repoType);
            postRepository.setType(repositoryType);
            repository= productsApi.postRepository(product.getId(), postRepository);

        }else {
            log.debug("Repository {} found", repoURL);
            repository = repo.get();
        }

        return repository;
    }

    private JobConfigurations createJobConfig(){
        JobConfigurations jobConfigurations= new JobConfigurations();
        log.debug("creating JobConfig");
        AnalyzerJobConfiguration analyzerJobConfiguration= new AnalyzerJobConfiguration();
        analyzerJobConfiguration.addEnabledPackageManagersItem("Bazel");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Maven");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Bower");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Bundler");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Cargo");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Carthage");
        analyzerJobConfiguration.addEnabledPackageManagersItem("CocoaPods");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Composer");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Conan");
        analyzerJobConfiguration.addEnabledPackageManagersItem("GoMod");
        analyzerJobConfiguration.addEnabledPackageManagersItem("GradleInspector");
        analyzerJobConfiguration.addEnabledPackageManagersItem("NPM");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Nuget");
        analyzerJobConfiguration.addEnabledPackageManagersItem("PIP");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Pipenv");
        analyzerJobConfiguration.addEnabledPackageManagersItem("PNPM");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Poetry");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Pub");
        analyzerJobConfiguration.addEnabledPackageManagersItem("SBT");
        analyzerJobConfiguration.addEnabledPackageManagersItem("SpdxDocumentFile");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Stack");
        analyzerJobConfiguration.addEnabledPackageManagersItem("SwiftPM");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Yarn");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Yarn2");
        analyzerJobConfiguration.addEnabledPackageManagersItem("Unmanaged");
        analyzerJobConfiguration.allowDynamicVersions(true);
        ProviderPluginConfiguration providerPluginConfiguration= new ProviderPluginConfiguration();
        providerPluginConfiguration.setType("OrtConfig");
        providerPluginConfiguration.putOptionsItem("OrtConfig", "true");
        analyzerJobConfiguration.addPackageCurationProvidersItem(providerPluginConfiguration);
        jobConfigurations.setAnalyzer(analyzerJobConfiguration);

        AdvisorJobConfiguration advisorJobConfiguration = new AdvisorJobConfiguration();
        advisorJobConfiguration.setAdvisors(List.of("OSV"));
        jobConfigurations.setAdvisor(advisorJobConfiguration);

        //Scancode is default scanner
        ScannerJobConfiguration scannerJobConfiguration= new ScannerJobConfiguration();
        jobConfigurations.setScanner(scannerJobConfiguration);

        EvaluatorJobConfiguration evaluatorJobConfiguration= new EvaluatorJobConfiguration();
        jobConfigurations.setEvaluator(evaluatorJobConfiguration);

        ReporterJobConfiguration reporterJobConfiguration = new ReporterJobConfiguration();
        reporterJobConfiguration.setFormats(List.of("SpdxDocument", "CycloneDx"));
        //set the config options, so you get json as output
        Map<String, String> options= new HashMap<>();
        options.put("outputFileFormats", "JSON");
        options.put("spdxVersion", "SPDX-2.3");
        Map<String, PluginConfig> pluginConfigMap= new HashMap<>();
        PluginConfig conf = new PluginConfig();
        conf.setOptions(options);
        pluginConfigMap.put("SpdxDocument", conf);
        reporterJobConfiguration.config(pluginConfigMap);
        jobConfigurations.setReporter(reporterJobConfiguration);

        return jobConfigurations;
    }


}
