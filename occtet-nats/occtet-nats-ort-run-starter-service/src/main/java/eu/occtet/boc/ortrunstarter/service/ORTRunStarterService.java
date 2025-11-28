package eu.occtet.boc.ortrunstarter.service;

import eu.occtet.boc.model.ORTRunWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.OrtRun;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ORTRunStarterService {

    private static final Logger log = LogManager.getLogger(ORTRunStarterService.class);

    String clientId="ort-server";
    private String tokenUrl="http://localhost:8081/realms/master/protocol/openid-connect/token";
    private String username = "ort-admin";
    private String password = "password";

    public boolean process(ORTRunWorkData workData) throws Exception {
        return startOrtRun(workData.getRunId());
    }

    boolean startOrtRun(long runId) throws IOException, InterruptedException, ApiException {
        OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
        AuthService authService = new AuthService(tokenUrl);
        TokenResponse tokenResponse = authService.requestToken(clientId,username,password,"offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        // demo code only! This only gets the run information, but does not start it. We need to figure out how that is done.
        RunsApi runsApi = new RunsApi(apiClient);
        OrtRun run = runsApi.getRun(runId);
        System.out.println(run);

        return true;

    }


}
