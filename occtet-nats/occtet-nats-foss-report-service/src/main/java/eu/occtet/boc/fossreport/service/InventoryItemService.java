package eu.occtet.boc.fossreport.service;

import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.fossreport.dao.InventoryItemRepository;
import eu.occtet.boc.fossreport.factory.InventoryItemFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class InventoryItemService {

    @Autowired
    private InventoryItemFactory inventoryItemFactory;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    public InventoryItem getOrCreateInventoryItem(String inventoryName, SoftwareComponent sc,
                                                  Project project) {
        List<InventoryItem> inventoryItemList = inventoryItemRepository.findByProjectAndInventoryName(
                project, inventoryName
        );
        if (inventoryItemList.isEmpty()) {
            return inventoryItemFactory.create(inventoryName, project, sc);
        }
        return inventoryItemList.getFirst(); // Return the first inventory of the inventories found
    }

    public InventoryItem getOrCreateInventoryItemWithAllAttributes(
            Project project,
            String inventoryName,
            Integer size,
            String linking,
            String externalNotes,
            InventoryItem parentItem,
            SoftwareComponent sc,
            Boolean wasCombined,
            List<Copyright> copyrights,
            String basePath,
            Integer priority
    ) {
        List<InventoryItem> inventoryItemList =
                inventoryItemRepository.findByProjectAndSoftwareComponentAndInventoryName(
                        project, sc, inventoryName
                );
        InventoryItem inventoryItem;

        if(inventoryItemList.isEmpty()){
            inventoryItem = inventoryItemFactory.create(
                    inventoryName, size, linking, externalNotes, parentItem, sc, wasCombined, copyrights,
                    project, basePath, priority);
        } else {
            inventoryItem = inventoryItemList.getFirst();
            updateInventoryItem(inventoryItem,
                    linking,  externalNotes, parentItem, sc, wasCombined, copyrights
            );
        }

        return inventoryItem;
    }

    private void updateInventoryItem(
            InventoryItem inventoryItem,
            String linking,
            String externalNotes,
            InventoryItem parentItem,
            SoftwareComponent component,
            Boolean wasCombined,
            List<Copyright> copyrights
    ) {
        if(!inventoryItem.getLinking().equals(linking) && linking != null && !linking.isEmpty()){
            inventoryItem.setLinking(linking);
        }

        if(externalNotes != null && !externalNotes.isEmpty()){
            if(inventoryItem.getExternalNotes() == null || inventoryItem.getExternalNotes().isEmpty()){
                inventoryItem.setExternalNotes(externalNotes);
            } else if(!inventoryItem.getExternalNotes().equals(externalNotes)){
                inventoryItem.setExternalNotes(inventoryItem.getExternalNotes() + "\n" + externalNotes);
            }
        }if(parentItem != null){
            inventoryItem.setParent(parentItem);
        }if(component != null) {
            inventoryItem.setSoftwareComponent(component);
        }if(wasCombined != null) {
            inventoryItem.setWasCombined(wasCombined);
        }if(copyrights != null && !copyrights.isEmpty()){
            HashSet<Copyright> updatedCopyrights = new HashSet<>(inventoryItem.getCopyrights());
            updatedCopyrights.addAll(copyrights);
            inventoryItem.setCopyrights(List.copyOf(updatedCopyrights));
        }

        inventoryItemRepository.save(inventoryItem);
    }
}
