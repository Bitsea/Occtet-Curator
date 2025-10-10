package eu.occtet.boc.spdx.factory;


import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.spdx.dao.SoftwareComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoftwareComponentFactory {

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    public SoftwareComponent create(String softwareName, String version,
                                    List<License> license,  String url) {
        String purl="";
        boolean curated=false;
        SoftwareComponent softwareComponent= new SoftwareComponent(softwareName, version,license, url );
        return softwareComponentRepository.save(softwareComponent);}

    public SoftwareComponent create(String softwareName, String version) {
        return softwareComponentRepository.save(new SoftwareComponent(softwareName,version));
    }
}
