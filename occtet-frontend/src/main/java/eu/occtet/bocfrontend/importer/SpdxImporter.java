package eu.occtet.bocfrontend.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.ImportStatus;
import eu.occtet.bocfrontend.entity.ImportTask;
import eu.occtet.bocfrontend.factory.ImportTaskFactory;
import eu.occtet.bocfrontend.service.ImportTaskService;
import eu.occtet.bocfrontend.service.NatsService;
import io.nats.client.api.ObjectInfo;
import io.nats.client.api.ObjectMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class SpdxImporter extends Importer {

    private static final Logger log = LogManager.getLogger(SpdxImporter.class);

    @Autowired
    private ImportTaskFactory importTaskFactory;


    @Autowired
    private NatsService natsService;

    @Autowired
    private ImportTaskFactory importTaskFactory1;

    protected SpdxImporter() {
        super("SPDX_Import");
    }


    private static final String CONFIG_KEY_USE_LICENSE_MATCHER = "UseLicenseMatcher";
    private static final String CONFIG_KEY_FILENAME= "fileName";
    private static final String CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER = "UseFalseCopyrightFilter";
    private static final boolean DEFAULT_USE_LICENSE_MATCHER = true;
    private static final boolean DEFAULT_USE_FALSE_COPYRIGHT_FILTER = true;


    @Override
    public boolean processTask(ImportTask importer) {

        try {
            log.debug("Processing SPDX Report: {}", importer.getStatus());

            byte[] spdxJson = new byte[0];
            boolean useCopyright = DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
            boolean useLicenseMatcher = DEFAULT_USE_LICENSE_MATCHER;
            String filename = "";
            List<Configuration> configurations = importer.getImportConfiguration();
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

            UUID projectId = importer.getProject().getId();

            sendIntoStream(spdxJson, projectId, useCopyright ,useLicenseMatcher, filename);

            return true;
        }catch (Exception e){
            log.error("Error when trying to send message to other microservice: {}", e.getMessage());
            importTaskFactory.saveWithFeedBack(importer,List.of("Error when trying to send message to other microservice: "+ e.getMessage()), ImportStatus.STOPPED);
            return false;
        }

    }

    private void sendIntoStream(byte[] spdxJson, UUID projectId, boolean useCopyright, boolean useLicenseMatch, String filename) {

        ByteArrayInputStream objectStoreInput = new ByteArrayInputStream(spdxJson);

        ObjectMeta objectMeta = ObjectMeta.builder(filename)
                .description("Spdxdocument for use by spdx-microservice")
                .chunkSize(32 * 1024)
                .build();

        ObjectInfo objectInfo = natsService.putDataIntoObjectStore(objectStoreInput, objectMeta);

        SpdxWorkData spdxWorkData = new SpdxWorkData( objectInfo.getObjectName(), objectInfo.getBucket(), projectId.toString(), useCopyright, useLicenseMatch);
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("processing_spdx", "uploaded spdx report to be turned into entities by spdx-microservice", actualTimestamp, spdxWorkData);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            String message = mapper.writeValueAsString(workTask);
            log.debug("sending message to spdx service: {}", message);
            natsService.sendWorkMessageToStream("work.spdx", message.getBytes(Charset.defaultCharset()));
        }catch(Exception e){
            log.error("Error with microservice connection: {}", e.getMessage());
        }
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
