package eu.occtet.boc.spdx.service;


import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.spdx.dao.InventoryItemRepository;
import eu.occtet.boc.spdx.factory.InventoryItemFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryItemService {

    private static final Logger log = LogManager.getLogger(InventoryItemService.class);


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



    public void update(InventoryItem inventoryItem){
        inventoryItemRepository.save(inventoryItem);
    }
}
