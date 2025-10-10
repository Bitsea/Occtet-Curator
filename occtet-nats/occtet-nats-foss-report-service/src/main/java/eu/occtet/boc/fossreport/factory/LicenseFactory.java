package eu.occtet.boc.fossreport.factory;


import eu.occtet.boc.entity.License;
import eu.occtet.boc.fossreport.dao.LicenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseFactory {

    private static final Logger log = LoggerFactory.getLogger(LicenseFactory.class);

    private final LicenseRepository licenseRepository;

    @Autowired
    public LicenseFactory(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    public License create(String licenseId, String licenseText){
        log.debug("Creating License with licenseId: {} and licenseText: {}", licenseId, licenseText);
        License license = new License(licenseId, licenseText);
        return licenseRepository.save(license);
    }
    public License createWithModified(String licenseId, String licenseText, Boolean modified){
        log.debug("Creating License with licenseId: {} and licenseText: {} and modified: {}", licenseId, licenseText, modified);
        License license = new License(licenseId, licenseText, modified);
        return licenseRepository.save(license);
    }

}
