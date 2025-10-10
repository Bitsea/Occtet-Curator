package eu.occtet.boc.fossreport.dao;


import eu.occtet.boc.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {

    List<License> findByLicenseType(String licenseType);

}
