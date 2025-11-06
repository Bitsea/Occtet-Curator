package eu.occtet.boc.ortrunstarter.service;

import eu.occtet.boc.model.ORTRunWorkData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.OrganizationsApi;
import org.openapitools.client.model.Organization;
import org.openapitools.client.model.PagedResponseOrganization;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ORTRunStarterService {

    private static final Logger log = LogManager.getLogger(ORTRunStarterService.class);

    public boolean process(ORTRunWorkData workData) {
        return startOrtRun(workData.getRunId());
    }


    public boolean createORTServerClientConfig() {
        return true;
    }

    public boolean startOrtRun(int runId) {


        return true;

    }

    public List<Organization> getOrganizations() throws ApiException {

        ApiClient client = getApiClient();
        OrganizationsApi api = new OrganizationsApi(client);
        PagedResponseOrganization organizations = api.getOrganizations(100, 0L, "", "");
        return organizations.getData();


    }

    @NotNull
    private static ApiClient getApiClient() {
        String clientId="ort-api";
        String clientSecret="mR1CnbbSgzOu63VydLuYL2mwhk5lDQU2";
        Map<String, String> parameters = new HashMap<>();
        String basepath="/";
        ApiClient client = new ApiClient(basepath,clientId, clientSecret, parameters);
        return client;
    }


}
