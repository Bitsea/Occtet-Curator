package eu.occtet.boc.fossreport.factory;

import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.fossreport.dao.SoftwareComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoftwareComponentFactory {

    private static final Logger log = LoggerFactory.getLogger(SoftwareComponentFactory.class);

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    public SoftwareComponent create(String softwareName, String version,
                                              List<License> license, String url) {
        log.debug("Creating Software Component with name: {}, version: {} and more...", softwareName, version);
            return softwareComponentRepository.save(new SoftwareComponent(
                    softwareName,
                    version,
                    license,
                    url));
    }

    public SoftwareComponent create(String softwareName, String version) {
        log.debug("Creating Software Component with name: {} and version: {}", softwareName, version);

        return softwareComponentRepository.save(new SoftwareComponent(softwareName,version));
    }
}
