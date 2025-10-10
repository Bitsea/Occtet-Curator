package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.Configuration;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class ConfigurationFactory {

    @Autowired
    private DataManager dataManager;

    /**
     * create configuration
     * @param name
     * @param value
     * @return
     */
    public Configuration create(@Nonnull String name, @Nullable String value) {
        Configuration configuration = dataManager.create(Configuration.class);
        configuration.setName(name);
        configuration.setValue(value);
        return configuration;
    }
}