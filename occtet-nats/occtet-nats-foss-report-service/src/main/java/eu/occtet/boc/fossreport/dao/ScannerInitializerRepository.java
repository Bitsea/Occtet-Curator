package eu.occtet.boc.fossreport.dao;


import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.ScannerInitializer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScannerInitializerRepository extends JpaRepository<ScannerInitializer, Long> {

    List<ScannerInitializer> findByInventoryItemOrderByLastUpdateDesc(InventoryItem inventoryItem);
    Optional<ScannerInitializer> findById(UUID id);
}
