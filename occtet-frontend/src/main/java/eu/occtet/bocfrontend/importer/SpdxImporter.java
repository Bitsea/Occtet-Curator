package eu.occtet.bocfrontend.importer;

import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.TaskStatus;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.service.NatsService;
import io.nats.client.api.ObjectInfo;
import io.nats.client.api.ObjectMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
public class SpdxImporter extends TaskParent {

    private static final Logger log = LogManager.getLogger(SpdxImporter.class);

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private  NatsService natsService;

    @Value("${nats.send-subject-spdx}")
    private String sendSubjectSpdx;

    protected SpdxImporter() {
        super("SPDX_Import");
    }


    private static final String CONFIG_KEY_USE_LICENSE_MATCHER = "UseLicenseMatcher";
    private static final String CONFIG_KEY_FILENAME= "fileName";
    private static final String CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER = "UseFalseCopyrightFilter";
    private static final boolean DEFAULT_USE_LICENSE_MATCHER = true;
    private static final boolean DEFAULT_USE_FALSE_COPYRIGHT_FILTER = true;


    @Override
    public boolean prepareAndStartTask(CuratorTask curatorTask) {

        try {
            log.debug("Processing SPDX Report: {}", curatorTask.getStatus());

            byte[] spdxJson = new byte[0];
            boolean useCopyright = DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
            boolean useLicenseMatcher = DEFAULT_USE_LICENSE_MATCHER;
            String filename = "";
            List<Configuration> configurations = curatorTask.getTaskConfiguration();
            for(Configuration configuration: configurations){
                switch (configuration.getName()) {
                    case CONFIG_KEY_FILENAME:
                        spdxJson = configuration.getUpload();
                        filename = configuration.getValue();
                        break;
                    case CONFIG_KEY_USE_LICENSE_MATCHER:
                        useLicenseMatcher = Boolean.parseBoolean(configuration.getValue());
                        break;
                    case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER:
                        useCopyright = Boolean.parseBoolean(configuration.getValue());
                        break;
                }
            }

            Long projectId = curatorTask.getProject().getId();

            return startTask(curatorTask, spdxJson, projectId, useCopyright ,useLicenseMatcher, filename);

        }catch (Exception e){
            log.error("Exception when sending task", e);
            curatorTaskService.saveWithFeedBack(curatorTask,List.of("Exception when sending task: "+ e.getMessage()), TaskStatus.CANCELLED);
            return false;
        }

    }

    private boolean startTask(CuratorTask task, byte[] spdxJson, Long projectId, boolean useCopyright,
                           boolean useLicenseMatch, String filename)  {

        ByteArrayInputStream objectStoreInput = new ByteArrayInputStream(spdxJson);

        ObjectMeta objectMeta = ObjectMeta.builder(filename)
                .description("Spdxdocument for use by spdx-microservice")
                .chunkSize(32 * 1024)
                .build();

        ObjectInfo objectInfo = natsService.putDataIntoObjectStore(objectStoreInput, objectMeta);
        if(objectInfo==null) return false;

        SpdxWorkData spdxWorkData = new SpdxWorkData( objectInfo.getObjectName(), objectInfo.getBucket(), projectId, useCopyright, useLicenseMatch);

        return curatorTaskService.saveAndRunTask(task,spdxWorkData,"uploaded spdx report to be turned into entities by spdx-microservice", sendSubjectSpdx);
    }

    @Override
    public List<String> getSupportedConfigurationKeys() {
        return List.of(CONFIG_KEY_FILENAME, CONFIG_KEY_USE_LICENSE_MATCHER, CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER);
    }

    @Override
    public List<String> getRequiredConfigurationKeys() {
        return List.of(CONFIG_KEY_FILENAME);
    }

    @Override
    public Configuration.Type getTypeOfConfiguration(String key) {
        log.debug("getTypeOfConfiguration called for key: {}", key);
        return switch (key) {
            case CONFIG_KEY_FILENAME -> Configuration.Type.FILE_UPLOAD;
            case CONFIG_KEY_USE_LICENSE_MATCHER, CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER -> Configuration.Type.BOOLEAN;
            default -> super.getTypeOfConfiguration(key);
        };
    }

    @Override
    public String getDefaultConfigurationValue(String k) {
        return switch (k) {
            case CONFIG_KEY_USE_LICENSE_MATCHER -> "" + DEFAULT_USE_LICENSE_MATCHER;
            case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER -> "" + DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
            default -> super.getDefaultConfigurationValue(k);
        };
    }
}
