package eu.occtet.boc.fossreport.service;


import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.fossreport.dao.SoftwareComponentRepository;
import eu.occtet.boc.fossreport.factory.SoftwareComponentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SoftwareComponentService {

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private SoftwareComponentFactory softwareComponentFactory;

    public SoftwareComponent getOrCreateSoftwareComponent(
            String softwareName,
            String version,
            List<License> license,
            String url){
        List<SoftwareComponent> existing = softwareComponentRepository.findByNameAndVersion(softwareName, version);

        SoftwareComponent softwareComponent;

        if (existing.isEmpty()) {
            softwareComponent = softwareComponentFactory.create(
                    softwareName, version, license, url);
        } else {
            // Ensure the existing software component is updated otherwise newly imported data could be lost.
            softwareComponent = existing.getFirst();
            updateSoftwareComponent(softwareComponent, license, url);
        }
        return softwareComponent;
    }

    public SoftwareComponent getOrCreateSoftwareComponent(String softwareName, String version){
        List<SoftwareComponent> softwareComponent = softwareComponentRepository.findByNameAndVersion(
                softwareName, version);
        if(softwareComponent.isEmpty()) {
            return softwareComponentFactory.create(softwareName, version);
        } else {
            return softwareComponent.getFirst();
        }
    }

    private void updateSoftwareComponent(
            SoftwareComponent softwareComponent,
            List<License> license, String url) {
        if (!softwareComponent.isCurated()) {
            if (license != null) {
                if (softwareComponent.getLicenses() == null) {
                    softwareComponent.setLicenses(license);
                } else
                    license.forEach(l -> {
                        if (!softwareComponent.getLicenses().contains(l))
                            softwareComponent.addLicense(l);
                    });
            }
            if (url != null && !url.isEmpty()) {
                softwareComponent.setDetailsUrl(url);
            }
            softwareComponentRepository.save(softwareComponent);
        }
    }
}
