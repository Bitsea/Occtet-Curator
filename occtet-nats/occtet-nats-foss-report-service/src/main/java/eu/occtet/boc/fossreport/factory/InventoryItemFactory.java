package eu.occtet.boc.fossreport.factory;


import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.fossreport.dao.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InventoryItemFactory {

    private static final Logger log = LoggerFactory.getLogger(InventoryItemFactory.class);

    private final InventoryItemRepository inventoryItemRepository;

    @Autowired
    public InventoryItemFactory(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public InventoryItem create(String inventoryName, int size, String linking,String externalNotes, InventoryItem parent,
                                SoftwareComponent component, boolean wasCombined, List<Copyright> copyrights,
                                Project project, String basePath, int priority) {
        log.debug("Creating InventoryItem with inventoryName: {}", inventoryName);

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setInventoryName(inventoryName);
        inventoryItem.setSize(size);
        inventoryItem.setLinking(linking);
        inventoryItem.setExternalNotes(externalNotes);
        inventoryItem.setParent(parent);
        inventoryItem.setSoftwareComponent(component);
        inventoryItem.setWasCombined(wasCombined);
        inventoryItem.setCopyrights(copyrights);
        inventoryItem.setProject(project);
        inventoryItem.setBasePath(basePath);
        inventoryItem.setPriority(priority);

        return inventoryItemRepository.save(inventoryItem);
    }


    public InventoryItem create(String inventoryName, Project project, SoftwareComponent sc) {
        return inventoryItemRepository.save(create(inventoryName, 0, "",
                "", null, sc, false,
                new ArrayList<>(), project, null, 0));
    }
}
