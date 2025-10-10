package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.ScannerInitializer;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ScannerInitializerRepository extends JmixDataRepository<ScannerInitializer, UUID> {

    List<ScannerInitializer> findByStatus(String status);
    long countByStatus(String status);
    Optional<ScannerInitializer>  findById(UUID id);
}
