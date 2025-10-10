package eu.occtet.boc.spdx.factory;



import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.spdx.dao.CodeLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CodeLocationFactory {

    @Autowired
    private CodeLocationRepository codeLocationRepository;

    public CodeLocation create(String filePath) {
        return codeLocationRepository.save(new CodeLocation(filePath));
    }

    public CodeLocation createWithInventory(String filePath, InventoryItem inventoryItem) {
        return codeLocationRepository.save(new CodeLocation(inventoryItem, filePath));
    }


}
