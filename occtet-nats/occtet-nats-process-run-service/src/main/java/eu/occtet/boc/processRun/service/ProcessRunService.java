package eu.occtet.boc.processRun.service;

import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.model.ORTProcessWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.Issue;
import org.openapitools.client.model.OrtRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ProcessRunService {

    private static final Logger log = LogManager.getLogger(ProcessRunService.class);

    @Autowired
    private ProjectRepository projectRepository;

    private String clientId="ort-server";
    private String tokenUrl="http://localhost:8081/realms/master/protocol/openid-connect/token";
    private String username = "ort-admin";
    private String password = "password";

    public boolean process(ORTProcessWorkData workData) throws Exception {
        return fetchRun(workData.getRunId(), workData.getProjectId());
    }

    public boolean fetchRun(long runId, long projectId) throws IOException, InterruptedException, ApiException {
        Project project= projectRepository.findById(projectId).get();

        OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
        AuthService authService = new AuthService(tokenUrl);

        TokenResponse tokenResponse = authService.requestToken(clientId,username,password,"offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        RunsApi runsApi = new RunsApi(apiClient);
        OrtRun run= runsApi.getRun(runId);
        List<Issue> issues= run.getIssues();


        return true;

    }
}
