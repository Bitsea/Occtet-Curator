package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.factory.ConfigurationFactory;
import eu.occtet.bocfrontend.engine.ScannerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

    private static final Logger log = LogManager.getLogger(ConfigurationService.class);

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ScannerManager scannerManager;

    public Configuration create(String name, String value){
        return configurationFactory.create(name, value);
    }

    public Configuration.Type getTypeOfConfiguration(String key, ScannerInitializer scannerInitializer) {
        return scannerManager.findScannerByName(scannerInitializer.getScanner()).getTypeOfConfiguration(key);
    }

    /**
     * Processes a given configuration object by applying the needed handler functions to it.
     * True is returned if the configuration was processed successfully, false otherwise.
     *
     * @param config the configuration object to be processed by the chain of handler functions
     */
    public boolean handleConfig(
            Configuration config,
            String nameField,
            byte[] uploadFieldValue,
            String uploadFileName,
            Boolean booleanField,
            ScannerInitializer scannerInitializer
    ) {
        if (getTypeOfConfiguration(nameField, scannerInitializer) == Configuration.Type.FILE_UPLOAD) {
            return handleFileUploadConfig(config, nameField, uploadFieldValue, uploadFileName);
        } else if (getTypeOfConfiguration(nameField, scannerInitializer) == Configuration.Type.BOOLEAN) {
            return handleValueOnly(config, nameField, booleanField.toString());
        } else {
            return handleValueOnly(config, nameField, config.getValue());
        }
    }


    private Boolean handleFileUploadConfig(
            Configuration config,
            String nameField,
            byte[] uploadFieldValue,
            String uploadFileName
    ) {
        log.debug("handle file upload config called with parameters nameField: {} and uploadField length: {}",
                nameField,
                uploadFieldValue.length);

        if (uploadFieldValue.length == 0) {
            log.error("upload invalid");
            return false;
        }

        config.setValue(uploadFileName);
        config.setUpload(uploadFieldValue);
        return true;
    }

    private Boolean handleValueOnly(
            Configuration config,
            String nameField,
            String valueField
    ) {
        log.debug("handle value only called with parameters nameField: {} and valueField: {}", nameField, valueField);
        config.setValue(valueField);
        return true;
    }
}
