package eu.occtet.bocfrontend.ortTask;

import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.bocfrontend.config.ConfigNatsProperties;
import eu.occtet.bocfrontend.config.ConfigOrtProperties;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.service.NatsService;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ProcessOrtRunTask {

    private static final Logger log = LogManager.getLogger(ProcessOrtRunTask.class);

    private final ConfigOrtProperties ortProperties;

    public ProcessOrtRunTask(ConfigOrtProperties ortProperties, ConfigNatsProperties natsProperties) {
        this.ortProperties = ortProperties;
        this.natsProperties = natsProperties;
    }

    private final ConfigNatsProperties natsProperties;


    @Value("${https.cacert.path}")
    private String cacertPath;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private CuratorTaskFactory curatorTaskFactory;

    private static final int MAX_PROCESSED_RUNS = 1000;
    private final Set<Long> processedRuns = Collections.synchronizedSet(
            Collections.newSetFromMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > MAX_PROCESSED_RUNS;
                }
            }));

    @Scheduled(cron = "${processRun.cron}")
    @Async
    public void fetchRun() {
        systemAuthenticator.withSystem(() -> {
            log.debug("trying to fetch finished runs from ORT API...");
            try {

                ApiClient apiClient = getApiClient();

                RunsApi runsApi = new RunsApi(apiClient);
                log.info("Fetching runs from ORT API: {}", runsApi.getApiClient().getBasePath());
                PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch = runsApi.getRuns("FINISHED", 10, null,
                        "-createdAt");

                PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearchWithIssues = runsApi
                        .getRuns("FINISHED_WITH_ISSUES", 10, null, "-createdAt");
                log.info("Runs fetched {}", pagedSearch.getData().size() + pagedSearchWithIssues.getData().size());

                if (!pagedSearch.getData().isEmpty()) {
                    log.info("Got {} finished runs", pagedSearch.getData().size());

                    sendRuns(pagedSearch);
                } else
                    log.debug("No finished runs found");

                if (!pagedSearchWithIssues.getData().isEmpty()) {
                    log.info("Got {} finished_with_issues runs", pagedSearchWithIssues.getData().size());
                    sendRuns(pagedSearchWithIssues);
                } else
                    log.debug("No finished_with_issues runs found");
            } catch (Exception e) {
                log.error("ORT API not reachable, could not fetch runs", e);
            }
            return null;
        });

    }

    private void sendRuns(PagedSearchResponseOrtRunSummaryOrtRunFilters pagedSearch) {
        if (pagedSearch.getData() == null)
            return;

        for (OrtRunSummary ortRunSummary : pagedSearch.getData()) {
            if (ortRunSummary == null)
                continue;
            Long summaryId = ortRunSummary.getId();
            if (summaryId == null)
                continue;

            // processedRuns.add() returns true only if the element was NOT already present
            if (processedRuns.add(summaryId)) {
                log.info("Found new finished ORT run with id {}, creating task", summaryId);
                CuratorTask task = curatorTaskFactory.create(null, "OrtResultTask", "processing_ort_run");
                ORTProcessWorkData ortProcessWorkData = new ORTProcessWorkData(summaryId);

                boolean res = curatorTaskService.saveAndRunTask(task, ortProcessWorkData,
                        "sending message and ort-runId to process-run-microservice", natsProperties.send_subject_ort_result());

                if (!res) {
                    log.info("Failed to start task for ORT run {}, removing from processed set to allow retry",
                            summaryId);
                    processedRuns.remove(summaryId);
                }
            } else {
                log.debug("ORT run {} already processed, skipping", summaryId);
            }
        }
    }

    private ApiClient getApiClient() {
        try {
            OrtClientService ortClientService = new OrtClientService(ortProperties.baseUrl(), cacertPath,
                    ortProperties.tokenUrl(), ortProperties.clientId());
            AuthService authService = new AuthService(ortProperties.tokenUrl(), cacertPath,
                    ortProperties.clientSecret());
            log.info("connection with ORT on {}", ortProperties.baseUrl());
            log.info("connection URL {}", ortProperties.tokenUrl());
            TokenResponse tokenResponse = null;

            tokenResponse = authService.requestToken(ortProperties.clientId(), ortProperties.username(),
                    ortProperties.password(), "openid");

            return ortClientService.createApiClient(tokenResponse);
        } catch (Exception e) {
            log.info("Error creating RunsApi client, ORT possibly not reachable/activated {}", e.getMessage());
            return null;
        }
    }

}
