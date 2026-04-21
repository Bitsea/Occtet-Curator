package eu.occtet.bocfrontend.ortTask;

import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.bocfrontend.config.ConfigOrtProperties;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SystemAuthenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.OrganizationsApi;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProcessOrtRunTask  {

    private static final Logger log = LogManager.getLogger(ProcessOrtRunTask.class);

    private final ConfigOrtProperties ortProperties;

    public ProcessOrtRunTask(ConfigOrtProperties ortProperties) {
        this.ortProperties = ortProperties;
    }

    @Value("${nats.send-subject-ort-result}")
    private String sendSubjectOrtResult;

    @Value("${https.cacert.path}")
    private String cacertPath;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private CuratorTaskFactory curatorTaskFactory;


    private List<Long> processedRuns= new ArrayList<>();


    @Scheduled(cron = "${processRun.cron}")
    @Async
    public void fetchRun()  {

        systemAuthenticator.withSystem(() -> {
            log.debug("trying to fetch finished runs from ORT API...");
            try {

                ApiClient apiClient = getApiClient();

                eu.occtet.bocfrontend.entity.User user = (eu.occtet.bocfrontend.entity.User) currentAuthentication.getUser();
                OrganizationsApi organizationsApi = new OrganizationsApi(apiClient);
                eu.occtet.bocfrontend.entity.Organization orga= user.getOrganization();

                if(orga == null){
                    log.error("User {} has no organization {} assigned, cannot fetch ORT runs", user.getUsername(),user.getOrganization());
                    return null;
                }
                Organization organization = createOrganization(orga.getOrganizationName(), organizationsApi);


                RunsApi runsApi = new RunsApi(apiClient);
                log.info("Fetching runs from ORT API: {} basepath api: {}", runsApi.getCustomBaseUrl(), runsApi.getApiClient().getBasePath());
                PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch = runsApi.getRuns("FINISHED", 1, null, "-createdAt");

                PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearchWithIssues = runsApi.getRuns("FINISHED_WITH_ISSUES", 1, null, "-createdAt");
                log.info("Runs fetched {}", pagedSearch.getData().size() + pagedSearchWithIssues.getData().size());

                if (!pagedSearch.getData().isEmpty()) {
                    log.debug("Got {} finished runs", pagedSearch.getData().size());

                    sendRuns(pagedSearch, organization.getId());
                } else log.debug("No finished runs found");

                if (!pagedSearchWithIssues.getData().isEmpty()) {
                    log.debug("Got {} finished_with_issues runs", pagedSearch.getData().size());
                    sendRuns(pagedSearchWithIssues, organization.getId());
                } else log.debug("No finished_with_issues runs found");
            } catch (Exception e) {
                log.error("ORT API not reachable, could not fetch runs", e);
            }
            return null;
        });

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
                log.info("Organization {} not found, creating it", orgaName);
                PostOrganization po = new PostOrganization();
                po.setName(orgaName);
                orga = organizationsApi.postOrganization(po);
            } else {
                log.info("Organization {} found", orgaName);
                orga = organization.get();
            }
            return orga;
        } catch (Exception e) {
            log.error("Error while cretating organization: {} with error: {}", orgaName, e.getMessage());
            throw e;
        }

    }

    private void sendRuns(PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch, Long orgaId){
        OrtRunSummary ortRunSummary = pagedSearch.getData().getFirst();
        if (ortRunSummary != null && !processedRuns.contains(ortRunSummary.getId()) && ortRunSummary.getOrganizationId().equals(orgaId)) {
            Long summaryId = ortRunSummary.getId();
            processedRuns.add(summaryId);
            log.info("Found new finished ORT run with id {}", summaryId);
            CuratorTask task = curatorTaskFactory.create(null, "OrtResultTask", "processing_ort_run");

            ORTProcessWorkData ortProcessWorkData = new ORTProcessWorkData(summaryId);

            boolean res = curatorTaskService.saveAndRunTask(task, ortProcessWorkData, "sending message and ort-runId to process-run-microservice", sendSubjectOrtResult);

            if (res) processedRuns.add(summaryId);
            else log.info("Failed to start task for ORT run {}", summaryId);
        }
    }



    private ApiClient getApiClient() {
        try {
            OrtClientService ortClientService = new OrtClientService(ortProperties.baseUrl(), cacertPath, ortProperties.tokenUrl(), ortProperties.clientId());
            AuthService authService = new AuthService(ortProperties.tokenUrl(), cacertPath);
            log.info("connection with ORT on {}", ortProperties.baseUrl());
            log.info("connection URL {}", ortProperties.tokenUrl());
            TokenResponse tokenResponse = null;
            log.info("authcall on keycloak with clientId {} username {} password {}", ortProperties.clientId(), ortProperties.username(), ortProperties.password() );

            tokenResponse = authService.requestToken(ortProperties.clientId(), ortProperties.username(), ortProperties.password(), "offline_access");


            return ortClientService.createApiClient(tokenResponse);
        }catch(Exception e){
            log.info("Error creating RunsApi client, ORT possibly not reachable/activated {}", e.getMessage());
            return null;
        }
    }


    @Scheduled(cron = "${processRun.cron}")
    @Async
    public void updateProcessedRuns(){

        if(!processedRuns.isEmpty()) {
            //delete alle runs from list, just not the recent one, so it will not processed over and over again
            processedRuns.subList(0, processedRuns.size() - 1).clear();
            log.debug("controll run {}, cleared processedRuns list, now size is {}", processedRuns.getFirst(), processedRuns.size());
        }
    }

}
