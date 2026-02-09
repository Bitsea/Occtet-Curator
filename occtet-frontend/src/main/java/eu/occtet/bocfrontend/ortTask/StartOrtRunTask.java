package eu.occtet.bocfrontend.ortTask;

import eu.occtet.boc.model.ORTStartRunWorkData;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.TaskStatus;
import eu.occtet.bocfrontend.importer.TaskParent;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.service.NatsService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StartOrtRunTask extends TaskParent {

    private static final Logger log = LogManager.getLogger(StartOrtRunTask.class);

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Value("${nats.send-subject-ort-run}")
    private String sendSubjectOrt;

    protected StartOrtRunTask() {
        super("ORT_Run_Starter");
    }

    //ORT server is needing organization, product and repository
    //as product we take the project
    private static final String CONFIG_KEY_ORGANIZATION = "Organization";
    private static final String CONFIG_KEY_REPOSITORY_TYPE = "RepositoryType";
    private static final String CONFIG_KEY_REPOSITORY_URL= "RepositoryURL";
    private static final String CONFIG_KEY_REPOSITORY_VERSION= "RepositoryVersion";


    @Override
    public boolean prepareAndStartTask(CuratorTask curatorTask) {

        try{
            log.debug("Processing SPDX Report: {}", curatorTask.getStatus());


            String repoType = "";
            String orgaName = "";
            String repoURL = "";
            String repoVersion = "";
            List<Configuration> configurations = curatorTask.getTaskConfiguration();

            for(Configuration configuration: configurations) {
                switch (configuration.getName()) {
                    case CONFIG_KEY_ORGANIZATION:
                        orgaName = configuration.getValue();
                        break;
                    case CONFIG_KEY_REPOSITORY_TYPE:
                        repoType = configuration.getValue();
                        break;
                    case CONFIG_KEY_REPOSITORY_URL:
                        repoURL= configuration.getValue();
                        break;
                    case CONFIG_KEY_REPOSITORY_VERSION:
                        repoVersion = configuration.getValue();
                        break;
                }

            }
            Long projectId = curatorTask.getProject().getId();

            return startTask(curatorTask, projectId, orgaName, repoType, repoURL, repoVersion);
        }catch (Exception e){
            log.error("Exception when sending task", e);
            curatorTaskService.saveWithFeedBack(curatorTask,List.of("Exception when sending task: "+ e.getMessage()), TaskStatus.CANCELLED);
            return false;
        }

    }

    private boolean startTask(CuratorTask task, Long projectId, String orgaName, String repoType, String repoURL, String repoVersion)  {


        ORTStartRunWorkData ortStartRunWorkData = new ORTStartRunWorkData(repoType, repoURL, repoVersion, orgaName, projectId);

        return curatorTaskService.saveAndRunTask(task,ortStartRunWorkData,"starting ORT run for project :" + projectId, sendSubjectOrt);
    }

    @Override
    public List<String> getSupportedConfigurationKeys() {
        return List.of(CONFIG_KEY_REPOSITORY_TYPE, CONFIG_KEY_ORGANIZATION, CONFIG_KEY_REPOSITORY_URL, CONFIG_KEY_REPOSITORY_VERSION);
    }

    @Override
    public List<String> getRequiredConfigurationKeys() {
        return List.of(CONFIG_KEY_REPOSITORY_TYPE, CONFIG_KEY_ORGANIZATION, CONFIG_KEY_REPOSITORY_URL, CONFIG_KEY_REPOSITORY_VERSION);
    }

    @Override
    public Configuration.Type getTypeOfConfiguration(String key) {
        log.debug("getTypeOfConfiguration called for key: {}", key);
        return switch (key) {
            case CONFIG_KEY_ORGANIZATION, CONFIG_KEY_REPOSITORY_URL, CONFIG_KEY_REPOSITORY_VERSION -> Configuration.Type.STRING;
            case CONFIG_KEY_REPOSITORY_TYPE -> Configuration.Type.REPOSITORY_TYPE;
            default -> super.getTypeOfConfiguration(key);
        };
    }

    @Override
    public String getDefaultConfigurationValue(String k) {
        return switch (k) {
            case CONFIG_KEY_ORGANIZATION, CONFIG_KEY_REPOSITORY_URL, CONFIG_KEY_REPOSITORY_VERSION -> "";
            case CONFIG_KEY_REPOSITORY_TYPE -> "";
            default -> super.getDefaultConfigurationValue(k);
        };
    }
}
