package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LicenseService {

    private static final Logger log = LogManager.getLogger(LicenseService.class);
    private final LicenseRepository licenseRepository;

    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private SoftwareComponentService softwareComponentService;

    public LicenseService(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    public List<License> findLicensesByProject(Project project){
        List<SoftwareComponent> softwareComponents = softwareComponentService.findSoftwareComponentsByProject(project);
        List<License> licenses = new ArrayList<>();
        softwareComponents.forEach(sc->licenses.addAll(sc.getLicenses()));
        return licenses;
    }

    public List<License> findLicenseByPriority(Integer priority){
        return licenseRepository.findLicensesByPriority(priority);
    }

    public List<License> findLicenseByCurated(Boolean isCurated){
        return licenseRepository.findLicensesByCurated(isCurated);
    }

}
