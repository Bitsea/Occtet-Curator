package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.InventoryItem;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class CodeLocationFactory {

    @Autowired
    private DataManager dataManager;

    public CodeLocation create(@Nonnull InventoryItem inventoryItem, @Nonnull String filePath,
                               @Nonnull Integer lineNumber,
                               @Nonnull Integer lineNumberTo) {
        CodeLocation codeLocation = dataManager.create(CodeLocation.class);
        codeLocation.setInventoryItem(inventoryItem);
        codeLocation.setFilePath(filePath);
        codeLocation.setLineNumberOne(lineNumber);
        codeLocation.setLineNumberTwo(lineNumberTo);
        return dataManager.save(codeLocation);
    }

    public CodeLocation create(@Nonnull InventoryItem inventoryItem, @Nonnull String filePath){
        return create(inventoryItem, filePath, 0, 0);
    }
}
