package eu.occtet.boc.ai.licenseMatcher.dao;

import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoftwareComponentRepository extends JpaRepository<SoftwareComponent, Long> {
}
