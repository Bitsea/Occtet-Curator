package eu.occtet.bocfrontend.factory;


import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Copyright;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyrightFactory {

    @Autowired
    private DataManager dataManager;

    public Copyright create(String copyrightName, CodeLocation codeLocation, boolean isCurated, boolean isGarbage){

        Copyright copyright = dataManager.create(Copyright.class);
        copyright.setCopyrightText(copyrightName);
        copyright.setCodeLocation(codeLocation);
        copyright.setCurated(isCurated);
        copyright.setGarbage(isGarbage);

        return dataManager.save(copyright);
    }
}
