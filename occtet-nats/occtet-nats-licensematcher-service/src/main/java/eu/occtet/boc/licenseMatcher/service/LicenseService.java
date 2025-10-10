package eu.occtet.boc.licenseMatcher.service;


import eu.occtet.boc.entity.License;
import eu.occtet.boc.licenseMatcher.dao.LicenseRepository;
import eu.occtet.boc.licenseMatcher.factory.LicenseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);


    @Autowired
    private LicenseFactory licenseFactory;

    @Autowired
    private LicenseRepository licenseRepository;


    public License findOrCreateLicense(String licenseId, String licenseText ) {
        List<License> license = licenseRepository.findByLicenseType(licenseId);


        if (!license.isEmpty()) {
            return license.getFirst();
        } else {

            return licenseFactory.create(licenseId, licenseText);
        }

    }

    public License findOrCreateLicense(String licenseId, String licenseText, String licenseName ) {
        List<License> license = licenseRepository.findByLicenseType(licenseId);


        if (!license.isEmpty()) {
            return license.getFirst();
        } else {

            return licenseFactory.createWithName(licenseId, licenseText, licenseName);
        }
    }



    public License findOrCreateLicenseWithModified(String licenseId, String licenseText, Boolean modified){
        List<License> license = licenseRepository.findByLicenseType(licenseId);

        if (!license.isEmpty()) {
            return license.getFirst();
        } else {

            return licenseFactory.createWithModified(licenseId, licenseText, modified);
        }
    }
}
