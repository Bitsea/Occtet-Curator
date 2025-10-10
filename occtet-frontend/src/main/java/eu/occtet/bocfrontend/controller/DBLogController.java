package eu.occtet.bocfrontend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class DBLogController {

    private final static String BACKEND_API = "http://localhost:8080/";

    private static RestClient restClient;

    public DBLogController() {
        restClient = RestClient.create();
    }

    public String callCopyrightApi(){
        String apiUrl = BACKEND_API+"copyright";


        return restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(String.class);
    }

    public String callLicenseApi(){
        String apiUrl = BACKEND_API+"license";

        return restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(String.class);
    }
}
