package eu.occtet.template.config;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import java.util.Map;

@Configuration
public class CustomizedEclipseLinkJpaVendorAdapter extends EclipseLinkJpaVendorAdapter {


    @Value("${spring.jpa.generate-ddl}")
    private boolean generateDdl;

    @Override
    public Map<String, Object> getJpaPropertyMap() {
        Map<String, Object> map= super.getJpaPropertyMap();
        map.put(PersistenceUnitProperties.WEAVING, "false");
        if(generateDdl)
            map.put(PersistenceUnitProperties.DDL_GENERATION, "create-or-extend-tables");
        else
            map.put(PersistenceUnitProperties.DDL_GENERATION, "none");

        return map;
    }

}
