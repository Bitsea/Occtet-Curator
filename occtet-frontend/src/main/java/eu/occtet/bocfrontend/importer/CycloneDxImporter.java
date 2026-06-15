package eu.occtet.bocfrontend.importer;

import eu.occtet.boc.model.CycloneDxWorkData;
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
public class CycloneDxImporter extends TaskParent {

    private static final Logger log = LogManager.getLogger(CycloneDxImporter.class);

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private  NatsService natsService;

    @Value("${nats.send-subject-cyclonedx}")
    private String sendSubjectCycloneDx;

    protected CycloneDxImporter() {
        super("CYCLONEDX_Import");
    }


    private static final String CONFIG_KEY_USE_LICENSE_MATCHER = "UseLicenseMatcher";
    private static final String CONFIG_KEY_FILENAME= "fileName";
    private static final String CONFIG_KEY_WITH_TEST_LIBRARIES= "withTestLibraries";
    private static final String CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER = "UseFalseCopyrightFilter";
    private static final boolean DEFAULT_USE_LICENSE_MATCHER = true;
    private static final boolean DEFAULT_USE_FALSE_COPYRIGHT_FILTER = true;
    private static final boolean DEFAULT_WITH_TEST_LIBRARIES= false;


    @Override
    public boolean prepareAndStartTask(CuratorTask curatorTask) {

        try {
            log.debug("Processing CycloneDx Report: {}", curatorTask.getStatus());

            byte[] cyclonedxJson = new byte[0];
            boolean useCopyright = DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
            boolean useLicenseMatcher = DEFAULT_USE_LICENSE_MATCHER;
            String filename = "";
            boolean withTestLibraries = DEFAULT_WITH_TEST_LIBRARIES;
            List<Configuration> configurations = curatorTask.getTaskConfiguration();
            for(Configuration configuration: configurations){
                switch (configuration.getName()) {
                    case CONFIG_KEY_FILENAME:
                        cyclonedxJson = configuration.getUpload();
                        filename = configuration.getValue();
                        break;
                    case CONFIG_KEY_USE_LICENSE_MATCHER:
                        useLicenseMatcher = Boolean.parseBoolean(configuration.getValue());
                        break;
                    case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER:
                        useCopyright = Boolean.parseBoolean(configuration.getValue());
                        break;
                    case CONFIG_KEY_WITH_TEST_LIBRARIES:
                        withTestLibraries= Boolean.parseBoolean(configuration.getValue());
                        break;
                }
            }

            Long projectId = curatorTask.getProject().getId();

            return startTask(curatorTask, cyclonedxJson, projectId, useCopyright ,useLicenseMatcher, filename, withTestLibraries);

        }catch (Exception e){
            log.error("Exception when sending task", e);
            curatorTaskService.saveWithFeedBack(curatorTask,List.of("Exception when sending task: "+ e.getMessage()), TaskStatus.CANCELLED);
            return false;
        }

    }

    private boolean startTask(CuratorTask task, byte[] cyclonedxJson, Long projectId, boolean useCopyright,
                           boolean useLicenseMatch, String filename, boolean withTestLibraries)  {

        ByteArrayInputStream objectStoreInput = new ByteArrayInputStream(cyclonedxJson);

        ObjectMeta objectMeta = ObjectMeta.builder(filename)
                .description("cycloneDx document for use by cyclonedx-microservice")
                .chunkSize(32 * 1024)
                .build();

        ObjectInfo objectInfo = natsService.putDataIntoObjectStore(objectStoreInput, objectMeta);
        if(objectInfo==null) return false;

        CycloneDxWorkData cycloneDxWorkData = new CycloneDxWorkData( objectInfo.getObjectName(), objectInfo.getBucket(), projectId, useCopyright, useLicenseMatch, withTestLibraries);

        return curatorTaskService.saveAndRunTask(task,cycloneDxWorkData,"uploaded cycloneDx report to be turned into entities by cyclonedx-microservice", sendSubjectCycloneDx);
    }

    @Override
    public List<String> getSupportedConfigurationKeys() {
        return List.of(CONFIG_KEY_FILENAME, CONFIG_KEY_USE_LICENSE_MATCHER, CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER, CONFIG_KEY_WITH_TEST_LIBRARIES);
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
            case CONFIG_KEY_USE_LICENSE_MATCHER, CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER, CONFIG_KEY_WITH_TEST_LIBRARIES -> Configuration.Type.BOOLEAN;
            default -> super.getTypeOfConfiguration(key);
        };
    }

    @Override
    public String getDefaultConfigurationValue(String k) {
        return switch (k) {
            case CONFIG_KEY_USE_LICENSE_MATCHER -> "" + DEFAULT_USE_LICENSE_MATCHER;
            case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER -> "" + DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
            case CONFIG_KEY_WITH_TEST_LIBRARIES -> "" + DEFAULT_WITH_TEST_LIBRARIES;
            default -> super.getDefaultConfigurationValue(k);
        };
    }
}
