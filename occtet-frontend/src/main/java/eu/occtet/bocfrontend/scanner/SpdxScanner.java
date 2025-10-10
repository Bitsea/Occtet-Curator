package eu.occtet.bocfrontend.scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;
import eu.occtet.bocfrontend.factory.ScannerInitializerFactory;
import eu.occtet.bocfrontend.service.NatsService;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


@Service
public class SpdxScanner extends Scanner{

    private static final Logger log = LogManager.getLogger(SpdxScanner.class);

    @Autowired
    private ScannerInitializerFactory scannerInitializerFactory;

    protected SpdxScanner() {
        super("Spdx_Scanner");
    }

    private static final String CONFIG_KEY_USE_LICENSE_MATCHER = "UseLicenseMatcher";
    private static final String CONFIG_KEY_FILENAME= "fileName";
    private static final String CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER = "UseFalseCopyrightFilter";
    private static final boolean DEFAULT_USE_LICENSE_MATCHER = true;
    private static final boolean DEFAULT_USE_FALSE_COPYRIGHT_FILTER = true;

    @Autowired
    private NatsService natsService;

    @Override
    public boolean processTask(@NotNull ScannerInitializer scannerInitializer, @NotNull Consumer<ScannerInitializer> completionCallback) {

        try {
            log.debug("Processing SPDX Report: {}", scannerInitializer.getStatus());

            byte[] spdxJson = new byte[0];
            boolean useCopyright = DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
            boolean useLicenseMatcher = DEFAULT_USE_LICENSE_MATCHER;
            List<Configuration> configurations = scannerInitializer.getScannerConfiguration();
            for(Configuration configuration: configurations){
                switch (configuration.getName()) {
                    case CONFIG_KEY_FILENAME:
                        spdxJson = configuration.getUpload();
                        break;
                    case CONFIG_KEY_USE_LICENSE_MATCHER:
                        useLicenseMatcher = Boolean.parseBoolean(configuration.getValue());
                        break;
                    case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER:
                        useCopyright = Boolean.parseBoolean(configuration.getValue());
                        break;
                }
            }

            UUID projectId = scannerInitializer.getInventoryItem().getProject().getId();
            UUID rootInventoryItemId = scannerInitializer.getInventoryItem().getId();

            sendIntoStream(spdxJson, projectId, rootInventoryItemId, useCopyright ,useLicenseMatcher);
            completionCallback.accept(scannerInitializer);
            return true;
        }catch (Exception e){
            log.error("Error when trying to send message to other microservice: {}", e.getMessage());
            scannerInitializerFactory.saveWithFeedBack(scannerInitializer,List.of("Error when trying to send message to other microservice: "+ e.getMessage()), ScannerInitializerStatus.STOPPED);
            return false;
        }

    }

    private void sendIntoStream(byte[] spdxJson, UUID projectId, UUID rootInventoryItemId, boolean useCopyright, boolean useLicenseMatch) {

        SpdxWorkData spdxWorkData = new SpdxWorkData(spdxJson, projectId.toString(), rootInventoryItemId.toString(), useCopyright, useLicenseMatch);
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
            log.error("Error with microservice connection: "+ e.getMessage());
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
    public String getDefaultConfigurationValue(String k, InventoryItem inventoryItem) {
        switch(k) {
            case CONFIG_KEY_USE_LICENSE_MATCHER: return ""+DEFAULT_USE_LICENSE_MATCHER;
            case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER: return ""+DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
        }
        return super.getDefaultConfigurationValue(k, inventoryItem);
    }

}
