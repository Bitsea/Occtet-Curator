package eu.occtet.boc.spdx.service;

import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.spdx.dao.SoftwareComponentRepository;
import eu.occtet.boc.spdx.factory.SoftwareComponentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SoftwareComponentService {

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private SoftwareComponentFactory softwareComponentFactory;


    public SoftwareComponent getOrCreateSoftwareComponent(String softwareName, String version){
        List<SoftwareComponent> softwareComponent = softwareComponentRepository.findByNameAndVersion(
                softwareName, version);
        if(softwareComponent.isEmpty()) {
            return softwareComponentFactory.create(softwareName, version);
        } else {
            return softwareComponent.getFirst();
        }
    }

    public SoftwareComponent update(SoftwareComponent softwareComponent){
        return softwareComponentRepository.save(softwareComponent);
    }


}
