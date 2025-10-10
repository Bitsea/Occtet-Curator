package eu.occtet.bocfrontend.dao;


import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;


public interface SoftwareComponentRepository extends JmixDataRepository<SoftwareComponent, UUID> {

    SoftwareComponent findByName(String softwareComponentName);
    List<SoftwareComponent> findAll();
    List<SoftwareComponent> findSoftwareComponentsByCurated(Boolean curated);
}
