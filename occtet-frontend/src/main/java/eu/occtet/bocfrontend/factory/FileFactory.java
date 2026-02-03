package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

public class FileFactory {

    @Autowired
    private DataManager dataManager;

    public File create(@Nonnull InventoryItem inventoryItem, @Nonnull String filePath) {
        File file = dataManager.create(File.class);
        file.setInventoryItem(inventoryItem);
        file.setProjectPath(filePath);
        return dataManager.save(file);
    }

}
