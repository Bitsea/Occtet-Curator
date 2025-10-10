package eu.occtet.boc.spdx.service;



import eu.occtet.boc.entity.License;
import eu.occtet.boc.spdx.dao.LicenseRepository;
import eu.occtet.boc.spdx.factory.LicenseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LicenseService {

    private static final Logger log = LogManager.getLogger(LicenseService.class);


    @Autowired
    private LicenseFactory licenseFactory;

    @Autowired
    private LicenseRepository licenseRepository;



    public License findOrCreateLicense(String licenseId, String licenseText, String licenseName ) {
        List<License> license = licenseRepository.findByLicenseTypeAndLicenseText(licenseId, licenseText);
        if (!license.isEmpty()) {
            return license.getFirst();
        } else {
            return licenseFactory.createWithName(licenseId, licenseText, licenseName);
        }
    }

}
