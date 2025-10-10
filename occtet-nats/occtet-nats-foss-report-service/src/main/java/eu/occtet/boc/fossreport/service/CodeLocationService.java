package eu.occtet.boc.fossreport.service;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.fossreport.dao.CodeLocationRepository;
import eu.occtet.boc.fossreport.dao.CopyrightRepository;
import eu.occtet.boc.fossreport.dao.InventoryItemRepository;
import eu.occtet.boc.fossreport.factory.CodeLocationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeLocationService {

    private static final Logger log = LoggerFactory.getLogger(CodeLocationService.class);


    @Autowired
    private CodeLocationFactory codeLocationFactory;

    @Autowired
    private CodeLocationRepository codeLocationRepository;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    public CodeLocation findOrCreateCodeLocationWithInventory(String filePath, InventoryItem inventoryItem) {
        return codeLocationFactory.createWithInventory(filePath, inventoryItem);
    }

    public void CreateCodeLocationsWithInventory(List<String> filePaths, InventoryItem inventoryItem) {
        filePaths.forEach(filePath -> codeLocationFactory.createWithInventory(filePath, inventoryItem));
    }

    public void deleteOldCodeLocationsOfInventoryItem(InventoryItem inventoryItem, CodeLocation basePathCodeLocation){
        List<CodeLocation> toBeDeletedCls = codeLocationRepository.findByInventoryItem(inventoryItem);
        if (toBeDeletedCls.isEmpty()) return;

        toBeDeletedCls.remove(basePathCodeLocation);

        for (CodeLocation cl : toBeDeletedCls) {
            List<Copyright> copyrights = copyrightRepository.findByCodeLocation(cl);
            for (Copyright c : copyrights) {
                c.setCodeLocation(null);
                log.debug("CodeLocation {} has been removed from copyright {}", cl.getFilePath(), c.getCopyrightText());
            }
            copyrightRepository.saveAll(copyrights);
            copyrightRepository.flush();
        }
        codeLocationRepository.deleteAll(toBeDeletedCls);
        codeLocationRepository.flush();
        log.debug("Deleted {} old code locations of inventory item: {}", toBeDeletedCls.size(),
                inventoryItem.getInventoryName());
    }
}
