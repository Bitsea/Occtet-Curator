package eu.occtet.bocfrontend.importer;

import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.ImportTask;

import java.util.Collections;
import java.util.List;

public abstract class Importer {

    private final String name;


    protected Importer(String name) {
        this.name = name;
    }

    /**
     *
     * @return the name of this scanner
     */
    public String getName() {
        return name;
    }


    /**
     * Process the given scanning task. This may happen in background.
     * @param importTask the import to be processed
     ** @return true on success, false if something went wrong.
     */
    public abstract boolean processTask(ImportTask importTask);

    /**
     *
     * @return list of supported settings for this scanner
     */
    public List<String> getSupportedConfigurationKeys() {return Collections.emptyList();
    }

    /**
     *
     * @return list of required settings for this scanner
     */
    public List<String> getRequiredConfigurationKeys() {
        return Collections.emptyList();
    }

    public boolean isConfigurationRequired(String key) {
        return getRequiredConfigurationKeys().contains(key);
    }

    public String getDefaultConfigurationValue(String k) {
        return "";
    };

    public Configuration.Type getTypeOfConfiguration(String key) {
        return Configuration.Type.STRING;
    }


}
