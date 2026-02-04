package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class FileFactory {

    @Autowired
    private DataManager dataManager;

    public File create(@Nonnull InventoryItem inventoryItem, @Nonnull String filePath, @Nonnull Project project) {
        File file = dataManager.create(File.class);
        file.setInventoryItem(inventoryItem);
        file.setProjectPath(filePath);
        file.setProject(project);
        return dataManager.save(file);
    }

}
