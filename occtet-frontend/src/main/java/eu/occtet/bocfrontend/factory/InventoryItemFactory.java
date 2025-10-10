package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InventoryItemFactory {

    @Autowired
    protected DataManager dataManager;


    public InventoryItem create(@Nonnull String inventoryName, int size, @Nonnull String linking,
                                @Nonnull List<Copyright> copyrights, @Nonnull String externalNotes,
                                @Nonnull InventoryItem parent, @Nonnull SoftwareComponent softwareComponent,
                                boolean wasCombined, boolean curated, @Nonnull Project project,
                                @Nonnull String basePath){
        InventoryItem inventoryItem = dataManager.create(InventoryItem.class);
        inventoryItem.setInventoryName(inventoryName);
        inventoryItem.setSize(size);
        inventoryItem.setLinking(linking);
        inventoryItem.setCopyrights(copyrights);
        inventoryItem.setExternalNotes(externalNotes);
        inventoryItem.setParent(parent);
        inventoryItem.setSoftwareComponent(softwareComponent);
        inventoryItem.setWasCombined(wasCombined);
        inventoryItem.setCurated(curated);
        inventoryItem.setProject(project);
        inventoryItem.setBasePath(basePath);

        return dataManager.save(inventoryItem);
    }


    public InventoryItem create(@Nonnull String inventoryName, SoftwareComponent softwareComponent, Project project){
        return create(inventoryName, 0, "", new ArrayList<>(), "", null, softwareComponent,
                false, false, project,
                "");
    }
}
