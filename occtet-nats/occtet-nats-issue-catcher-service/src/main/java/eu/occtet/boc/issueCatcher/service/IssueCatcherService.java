package eu.occtet.boc.issueCatcher.service;

import com.squareup.okhttp.*;
import eu.occtet.boc.model.ORTRunWorkData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.awt.*;

@Service
public class IssueCatcherService {

    private static final Logger log = LogManager.getLogger(IssueCatcherService.class);

    public boolean process(ORTRunWorkData workData){
        return fetchIssues(workData.getRunId());
    }

    public boolean fetchIssues(int runId){
            String jsonResponse="";
            try {
                //use ORT API to fetch issues of a ort run
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = RequestBody.create(mediaType, "");
                Request request = new Request.Builder()
                        .url("http://localhost:8080/api/v1/"+runId+"/issues")
                        .method("GET", body)
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer <token>")
                        .build();
                Response response = client.newCall(request).execute();
                jsonResponse= response.body().string();
            }catch(Exception e){
                log.error("Could not fetch issues for ORT run with id {}: {}", runId, e.getMessage());
                return false;
            }


            return true;

        }
}
