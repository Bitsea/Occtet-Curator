package eu.occtet.boc.fossreport.service;


import eu.occtet.boc.entity.License;
import eu.occtet.boc.fossreport.dao.LicenseRepository;
import eu.occtet.boc.fossreport.factory.LicenseFactory;
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

    public License findOrCreateLicenseWithModified(String licenseId, String licenseText, Boolean modified){
        List<License> license = licenseRepository.findByLicenseType(licenseId);

        if (!license.isEmpty()) {
            return license.getFirst();
        } else {

            return licenseFactory.createWithModified(licenseId, licenseText, modified);
        }
    }
}
