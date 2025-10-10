package eu.occtet.bocfrontend.controller;

import eu.occtet.bocfrontend.scanner.FlexeraReportScanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;


@RestController
public class FlexeraReportController {
    private static final Logger log = LogManager.getLogger(FlexeraReportScanner.class);


    private final static String BACKEND_API = "http://localhost:8080";

    public String startFlexeraReportWorkflow(String inventoryItemName, String projectName) {

        String apiUrl = BACKEND_API+"/fossreport/"+inventoryItemName+"/"+projectName;
        log.debug("Calling Flexera Report API at URL: {}", apiUrl);

        RestClient restClient = RestClient.create();

        return restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(String.class);


    }

}
