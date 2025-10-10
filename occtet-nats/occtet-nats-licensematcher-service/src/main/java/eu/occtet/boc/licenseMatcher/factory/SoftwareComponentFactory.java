package eu.occtet.boc.licenseMatcher.factory;

import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.licenseMatcher.dao.SoftwareComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoftwareComponentFactory {

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    public SoftwareComponent create(String softwareName, String version,
                                              List<License> license, String severity,
                                              String cveDictionaryEntry) {
        String purl="";
        boolean curated=false;
        return softwareComponentRepository.save(new SoftwareComponent(
                softwareName,
                version,
                purl,
                curated,
                license, ""));
    }

    public SoftwareComponent create(String softwareName, String version) {
        return softwareComponentRepository.save(new SoftwareComponent(softwareName,version));
    }
}
