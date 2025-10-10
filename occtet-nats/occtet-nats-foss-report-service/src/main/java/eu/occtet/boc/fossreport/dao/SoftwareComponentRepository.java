package eu.occtet.boc.fossreport.dao;


import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoftwareComponentRepository extends JpaRepository<SoftwareComponent, Long> {

    List<SoftwareComponent> findByNameAndVersion(String softwareName, String version);
}
