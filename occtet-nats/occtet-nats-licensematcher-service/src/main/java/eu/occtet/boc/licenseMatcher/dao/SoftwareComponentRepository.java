package eu.occtet.boc.licenseMatcher.dao;


import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SoftwareComponentRepository extends JpaRepository<SoftwareComponent, Long> {

    List<SoftwareComponent> findByName(String softwareName);

    List<SoftwareComponent> findByNameAndVersion(String softwareName, String version);

    List<SoftwareComponent> findById(UUID id);
}
