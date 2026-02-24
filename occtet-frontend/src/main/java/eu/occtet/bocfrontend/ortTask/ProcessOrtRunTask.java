package eu.occtet.bocfrontend.ortTask;

import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.model.VulnerabilityServiceWorkData;
import eu.occtet.boc.model.WorkTaskProgress;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.importer.TaskManager;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.service.WorkTaskProgressMonitor;
import io.jmix.core.security.SystemAuthenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.OrtRun;
import org.openapitools.client.model.OrtRunSummary;
import org.openapitools.client.model.PagedSearchResponseOrtRunSummaryOrtRunFilters;
import org.openapitools.client.model.ReporterJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProcessOrtRunTask  {

    private static final Logger log = LogManager.getLogger(ProcessOrtRunTask.class);

    private String clientId="ort-server";
    private String tokenUrl="http://localhost:8081/realms/master/protocol/openid-connect/token";
    private String username = "ort-admin";
    private String password = "password";

    @Value("${nats.send-subject-ort-result}")
    private String sendSubjectOrtResult;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private CuratorTaskFactory curatorTaskFactory;

    @Autowired
    private WorkTaskProgressMonitor workTaskProgressMonitor;

    private List<Long> processedRuns= new ArrayList<>();


    @Scheduled(cron = "${processRun.cron}")
    public void fetchRun() throws IOException, InterruptedException, ApiException {

        systemAuthenticator.withSystem(() -> {
            try {
            OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
            AuthService authService = new AuthService(tokenUrl);

            TokenResponse tokenResponse = null;

                tokenResponse = authService.requestToken(clientId, username, password, "offline_access");

            ApiClient apiClient = ortClientService.createApiClient(tokenResponse);


            RunsApi runsApi = new RunsApi(apiClient);
            PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch = runsApi.getRuns("FINISHED", 1, null, "-createdAt");

            PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch1 = runsApi.getRuns("FINISHED_WITH_ISSUES", 1, null, "-createdAt");

            if (!pagedSearch.getData().isEmpty()) {
                OrtRunSummary ortRunSummary = pagedSearch.getData().getFirst();
                log.debug("Got {} finished runs", pagedSearch.getData().size());
                if (ortRunSummary != null && !processedRuns.contains(ortRunSummary.getId())) {
                    Long summaryId = ortRunSummary.getId();
                    processedRuns.add(summaryId);
                    log.debug("Found new finished ORT run with id {}", summaryId);
                    CuratorTask task = curatorTaskFactory.create(null, "OrtResultTask", "processing_ort_run");

                    ORTProcessWorkData ortProcessWorkData = new ORTProcessWorkData(summaryId);

                    boolean res = curatorTaskService.saveAndRunTask(task, ortProcessWorkData, "sending software component to process run microservice", sendSubjectOrtResult);

                    if (res) processedRuns.add(summaryId);
                    else log.debug("Failed to start task for ORT run {}", summaryId);
                }
            } else log.debug("No finished runs found");

            if (!pagedSearch1.getData().isEmpty()) {
                OrtRunSummary ortRunSummary = pagedSearch1.getData().getFirst();
                log.debug("Got {} finished with issues runs", pagedSearch1.getData().size());
                if (ortRunSummary != null && !processedRuns.contains(ortRunSummary.getId())) {
                    Long summaryId = ortRunSummary.getId();
                    processedRuns.add(summaryId);
                    log.debug("Found new finished_with_issues ORT run with id {}", summaryId);
                    CuratorTask task = curatorTaskFactory.create(null, "OrtResultTask", "processing_ort_run");

                    ORTProcessWorkData ortProcessWorkData = new ORTProcessWorkData(summaryId);

                    boolean res = curatorTaskService.saveAndRunTask(task, ortProcessWorkData, "sending software component to process run microservice", sendSubjectOrtResult);

                    if (res) processedRuns.add(summaryId);
                    else log.debug("Failed to start task for ORT run {}", summaryId);
                }
            } else log.debug("No finished_with_issues runs found");
        } catch (Exception e){
                throw new RuntimeException(e);
            }
            return null;
        });

    }


    @Scheduled(cron = "${processRun.cron}")
    public void updateProcessedRuns(){
        List<WorkTaskProgress> list= workTaskProgressMonitor.getAllProgress();
        log.debug("Found {} tasks in progress", list.size());
        for(WorkTaskProgress progress:list){
            progress.getName();
            log.debug("checking progress for task {} with status {}", progress.getName(), progress.getStatus());
        }
        //FIXME this is only now for testing
        processedRuns= new ArrayList<>();

    }

}
