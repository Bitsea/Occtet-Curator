package eu.occtet.bocfrontend.dao;


import eu.occtet.bocfrontend.entity.Configuration;
import io.jmix.core.repository.JmixDataRepository;

import java.util.UUID;


public interface ConfigurationRepository extends JmixDataRepository<Configuration, UUID> {
    public Configuration findByNameAndValue(String name, String value);
}
