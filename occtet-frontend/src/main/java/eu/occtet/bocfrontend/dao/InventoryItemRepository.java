package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryItemRepository extends JmixDataRepository<InventoryItem, UUID> {

    List<InventoryItem> findAll();
    List<InventoryItem> findBySoftwareComponent(SoftwareComponent softwareComponent);
    List<InventoryItem> findInventoryItemBySoftwareComponentOrderByCreatedAtDesc(SoftwareComponent softwareComponent);
    List<InventoryItem> findByProject(Project project);
    List<InventoryItem> findInventoryItemsByCurated(Boolean curated);
    List<InventoryItem> searchInventoryItemsBySoftwareComponentIsNotNull();
    List<InventoryItem> searchInventoryItemsBySoftwareComponent_Licenses(License license);
    List<InventoryItem> findInventoryItemsByInventoryNameAndSoftwareComponent(String name,SoftwareComponent softwareComponent);
    List<InventoryItem> findInventoryItemsByParent(InventoryItem item);
}
