package eu.occtet.boc.ai.licenseMatcher.dao;


import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findById(UUID id);

    List<InventoryItem> findByProjectAndInventoryName(Project project, String inventoryName);

    List<InventoryItem> findByProjectAndSoftwareComponentAndInventoryName(Project project, SoftwareComponent sc, String inventoryName);
}
