package eu.occtet.bocfrontend.ortTask;

import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.bocfrontend.config.ConfigOrtProperties;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import io.jmix.core.security.SystemAuthenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
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

@Service
public class ProcessOrtRunTask  {

    private static final Logger log = LogManager.getLogger(ProcessOrtRunTask.class);

    private final ConfigOrtProperties ortProperties;

    public ProcessOrtRunTask(ConfigOrtProperties ortProperties) {
        this.ortProperties = ortProperties;
    }

    @Value("${nats.send-subject-ort-result}")
    private String sendSubjectOrtResult;

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
            try {

                ApiClient apiClient= getApiClient();

                OrganizationsApi organizationsApi = new OrganizationsApi(apiClient);
                Organization orga= createOrganization(orgaName, organizationsApi);

                RunsApi runsApi = new RunsApi(apiClient);
                PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch = runsApi.getRuns("FINISHED", 1, null, "-createdAt");

            PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearchWithIssues = runsApi.getRuns("FINISHED_WITH_ISSUES", 1, null, "-createdAt");

            if (!pagedSearch.getData().isEmpty()) {
                log.debug("Got {} finished runs", pagedSearch.getData().size());

                sendRuns(pagedSearch);
            } else log.debug("No finished runs found");

            if (!pagedSearchWithIssues.getData().isEmpty()) {
                log.debug("Got {} finished_with_issues runs", pagedSearch.getData().size());
                sendRuns(pagedSearchWithIssues);
            } else log.debug("No finished_with_issues runs found");
            } catch (Exception e){
                log.error("ORT API not reachable, could not fetch runs", e.getMessage());
            }
        return null;
        });

    }

    private void sendRuns(PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch){
        OrtRunSummary ortRunSummary = pagedSearch.getData().getFirst();
        if (ortRunSummary != null && !processedRuns.contains(ortRunSummary.getId())) {
            Long summaryId = ortRunSummary.getId();
            processedRuns.add(summaryId);
            log.debug("Found new finished ORT run with id {}", summaryId);
            CuratorTask task = curatorTaskFactory.create(null, "OrtResultTask", "processing_ort_run");

            ORTProcessWorkData ortProcessWorkData = new ORTProcessWorkData(summaryId);

            boolean res = curatorTaskService.saveAndRunTask(task, ortProcessWorkData, "sending message and ort-runId to process-run-microservice", sendSubjectOrtResult);

            if (res) processedRuns.add(summaryId);
            else log.debug("Failed to start task for ORT run {}", summaryId);
        }
    }



    private ApiClient getApiClient() {
        try {
            OrtClientService ortClientService = new OrtClientService(ortProperties.baseUrl());
            AuthService authService = new AuthService(ortProperties.tokenUrl());
            log.debug("connection with ORT on {}", ortProperties.baseUrl());
            TokenResponse tokenResponse = null;
            log.debug("authcall on keycloak with clientId {} username {} password {}", ortProperties.clientId(), ortProperties.username(), ortProperties.password() );

            tokenResponse = authService.requestToken(ortProperties.clientId(), ortProperties.username(), ortProperties.password(), "offline_access");


            return ortClientService.createApiClient(tokenResponse);
        }catch(Exception e){
            log.error("Error creating RunsApi client, ORT possibly not reachable/activated {}", e.getMessage());
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
