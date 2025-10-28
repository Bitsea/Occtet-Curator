package eu.occtet.boc.ortrunstarter.service;

import com.squareup.okhttp.*;
import eu.occtet.boc.model.ORTRunWorkData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.apoapsis.ortserver.client.OrtServerClientConfig;
import org.springframework.stereotype.Service;

@Service
public class ORTRunStarterService {

    private static final Logger log = LogManager.getLogger(ORTRunStarterService.class);

    public boolean process(ORTRunWorkData workData){
        return startOrtRun(workData.getRunId());
    }


    public boolean createORTServerClientConfig(){
        return true;
    }

    public boolean startOrtRun(int runId){



            return true;

        }
}
