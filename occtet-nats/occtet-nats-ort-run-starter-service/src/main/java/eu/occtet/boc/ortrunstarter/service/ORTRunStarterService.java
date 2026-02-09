package eu.occtet.boc.ortrunstarter.service;

import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.model.ORTStartRunWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.OrganizationsApi;
import org.openapitools.client.api.ProductsApi;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.occtet.boc.entity.Project;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ORTRunStarterService {

    private static final Logger log = LogManager.getLogger(ORTRunStarterService.class);

    @Autowired
    private ProjectRepository projectRepository;

    String clientId="ort-server";
    private String tokenUrl="http://localhost:8081/realms/master/protocol/openid-connect/token";
    private String username = "ort-admin";
    private String password = "password";

    public boolean process(ORTStartRunWorkData workData) throws Exception {
        return startOrtRun(workData.getProjectId(), workData.getOrganizationName(), workData.getRepositoryType(), workData.getRepositoryUrl(), workData.getRepositoryVersion());
    }

    boolean startOrtRun(long projectId, String orgaName, String repoName, String repoURL, String repoType) throws IOException, InterruptedException, ApiException {
        Project project= projectRepository.getById(projectId);

        OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
        AuthService authService = new AuthService(tokenUrl);
        TokenResponse tokenResponse = authService.requestToken(clientId,username,password,"offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        //First check if orga is already existing
        // how to access organizations api
        OrganizationsApi organizationsApi = new OrganizationsApi(apiClient);
        PagedResponseOrganization orgs = organizationsApi.getOrganizations(null,null,null,orgaName);
        List<Organization> data = orgs.getData();
        Optional<Organization> organization= data.stream().filter(o -> o.getName().equals(orgaName)).findFirst();

        Organization orga= null;
        if(data.isEmpty() || organization.isEmpty()) {
            // nothing there? create an organization
            log.debug("Organization {} not found, creating it", orgaName);
            PostOrganization po = new PostOrganization();
            po.setName(orgaName);
            orga= organizationsApi.postOrganization(po);
        } else {
            log.debug("Organization {} found", orgaName);
            orga= organization.get();
        }



        //check if product /project is existing
        ProductsApi productsApi = new ProductsApi(apiClient);

        PagedResponseProduct pagedResponseProduct = organizationsApi.getOrganizationProducts(orga.getId(), null, null, null, project.getProjectName());
        List<Product> dataProd = pagedResponseProduct.getData();
        Optional<Product> prod= dataProd.stream().filter(p -> p.getName().equals(project.getProjectName())).findFirst();
        Product product= null;

        if(data.isEmpty() || prod.isEmpty()) {
            log.debug("Product {} not found, creating it", project.getProjectName());
            PostProduct pro= new PostProduct();
            pro.setName(project.getProjectName());
            product= organizationsApi.postProduct(orga.getId(), pro);
        } else {
            log.debug("Product {} found", project.getProjectName());
            product= prod.get();
        }


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

        //TODO how do i set the repository version??
        log.debug("creating postRun for repositoryType {} with url {}", repository.getType(), repository.getUrl());
        PostRepositoryRun postRepositoryRun= new PostRepositoryRun();
        postRepositoryRun.setRepositoryIds(List.of(repository.getId()));
        //is this how i start the run?
        List<OrtRun> runs = productsApi.postProductRuns(product.getId(), postRepositoryRun);
        runs.stream().forEach(run-> log.debug("Started run with id {} crated at {} with productid {} repoId {} ", run.getId(),run.getCreatedAt(), run.getProductId(), run.getRepositoryId()));

        return true;

    }




}
