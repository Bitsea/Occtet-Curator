package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SoftwareComponentFactory {

    @Autowired
    private DataManager dataManager;

    public SoftwareComponent create(@Nonnull String name, @Nonnull String version, @Nonnull String purl,
                                    boolean curated, List<License> licenses){
        SoftwareComponent softwareComponent = dataManager.create(SoftwareComponent.class);

        softwareComponent.setName(name);
        softwareComponent.setVersion(version);
        softwareComponent.setPurl(purl);
        softwareComponent.setCurated(curated);
        softwareComponent.setLicenses(licenses);

        return dataManager.save(softwareComponent);
    }

    public SoftwareComponent create(String name, String version){
        return create(name, version, "", false, new ArrayList<>());
    }
}