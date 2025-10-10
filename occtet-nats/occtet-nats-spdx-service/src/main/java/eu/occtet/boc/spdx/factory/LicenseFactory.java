package eu.occtet.boc.spdx.factory;



import eu.occtet.boc.entity.License;
import eu.occtet.boc.spdx.dao.LicenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseFactory {

    private final LicenseRepository licenseRepository;

    @Autowired
    public LicenseFactory(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    public License create(String licenseId, String licenseText){
        License license = new License(licenseId, licenseText);
        return licenseRepository.save(license);
    }

    public License createWithName(String licenseId, String licenseText, String licenseName){
        License license = new License(licenseId, licenseText, licenseName);
        return licenseRepository.save(license);
    }
}
