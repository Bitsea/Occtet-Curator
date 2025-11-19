package eu.occtet.boc.ortrunstarter.service;

import eu.occtet.boc.model.ORTRunWorkData;
import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.eclipse.apoapsis.ortserver.client.OrtServerClient;
import org.eclipse.apoapsis.ortserver.client.OrtServerClientConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ORTRunStarterService {

    private static final Logger log = LogManager.getLogger(ORTRunStarterService.class);

    String clientId="ort-server";
    String clientSecret="JTt98nrZK0yVsrRHxpyFUcy9x2RlAFPL";
    private String tokenUrl="http://localhost:8081/realms/master/protocol/openid-connect/token";
    private String username = "ort-server";
    private String password = "ort-server";

    public boolean process(ORTRunWorkData workData) {
        return startOrtRun(workData.getRunId());
    }


    public boolean createORTServerClientConfig() {
        return true;
    }

    public boolean startOrtRun(int runId) {

        OrtServerClientConfig ortServerClientConfig = new OrtServerClientConfig("http://localhost:8080",
                clientId, tokenUrl, username, password );

        OrtServerClient client = OrtServerClient.Companion.create(ortServerClientConfig);

        client.getVersions().getVersions(new Continuation<>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {
                if (o instanceof Result.Failure)
                    consumeException((((Result.Failure) o).exception));
                else
                    consumeResult((Map<String, String>) o);
            }

            private void consumeResult(Map<String,String> o) {
                System.out.println("result...");
                for(String key: o.keySet()) {
                    System.out.println(key + "→" + o.get(key));
                }
            }

            private void consumeException(Throwable exception) {
                System.out.println(exception);
            }
        });



        return true;

    }


  /*  public List<Organization> getOrganizations() throws ApiException {

        ApiClient client = getApiClient();
        OrganizationsApi api = new OrganizationsApi(client);
        PagedResponseOrganization organizations = api.getOrganizations(100, 0L, "", "");
        return organizations.getData();


    }*/
/*
    @NotNull
    private static ApiClient getApiClient() {

        Map<String, String> parameters = new HashMap<>();
        String basepath="/";
        ApiClient client = new ApiClient(basepath,clientId, clientSecret, parameters);
        return client;
    }*/


}
