package eu.occtet.bocfrontend.ortTask;

import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.Project;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.RunsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

@Service
public class ProcessOrtRunTask  {

    private String clientId="ort-server";
    private String tokenUrl="http://localhost:8081/realms/master/protocol/openid-connect/token";
    private String username = "ort-admin";
    private String password = "password";

    @Autowired
    private ProjectRepository projectRepository;


    @Scheduled(fixedDelayString = "${processRun.delay}")
    public boolean fetchRun(@Nonnull CuratorTask curatorTask) throws IOException, InterruptedException, ApiException {


        OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
        AuthService authService = new AuthService(tokenUrl);

        TokenResponse tokenResponse = authService.requestToken(clientId,username,password,"offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        List<Project> projects= projectRepository.findAll();

        List<Long> projectIds= projects.stream().map(Project::getId).toList();


        //TODO fetch all runs of a project and check if finished, then send a worktask to process-run-service
        RunsApi runsApi = new RunsApi(apiClient);
        runsApi.getRun(123L).getStatus();

        return false;
    }
}
