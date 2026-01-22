package eu.occtet.boc.dao;

import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SoftwareComponentRepository extends JpaRepository<SoftwareComponent, Long> {

    List<SoftwareComponent> findByNameAndVersion(String softwareName, String version);

}
