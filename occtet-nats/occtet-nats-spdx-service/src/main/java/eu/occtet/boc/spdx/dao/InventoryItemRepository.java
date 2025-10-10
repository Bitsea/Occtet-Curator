package eu.occtet.boc.spdx.dao;



import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByInventoryNameAndSoftwareComponent(String inventoryName, SoftwareComponent sc);

    List<InventoryItem> findBySoftwareComponentAndProject(SoftwareComponent softwareComponent, Project project);


    List<InventoryItem> findBySoftwareComponent(SoftwareComponent sc);

    List<InventoryItem> findByProjectAndSoftwareComponent(Project project, SoftwareComponent sc);

    List<InventoryItem> findByProjectAndInventoryName(Project project, String inventoryName);

    List<InventoryItem> findByProjectAndSoftwareComponentAndInventoryName(Project project, SoftwareComponent sc, String inventoryName);

    List<InventoryItem> findBySpdxIdAndProject(String spdxID, Project project);
    Optional<InventoryItem> findById(UUID uid);
}
