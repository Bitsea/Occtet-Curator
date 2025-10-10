package eu.occtet.bocfrontend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class SPXScannerController {


    private final static String BACKEND_API = "http://localhost:8080/";

    public String startSpdxWorkflow(){

            String apiUrl = BACKEND_API+"consumeSpdx/";

            RestClient restClient = RestClient.create();

            return restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(String.class);


    }


}
