package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.License;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;

public interface LicenseRepository  extends JmixDataRepository<License, UUID> {

    List<License> findAll();
    List<License> findByLicenseName(String licenseName);
    List<License> findLicensesByCurated(Boolean curated);
    List<License> findLicensesByPriority(Integer priority);
}
