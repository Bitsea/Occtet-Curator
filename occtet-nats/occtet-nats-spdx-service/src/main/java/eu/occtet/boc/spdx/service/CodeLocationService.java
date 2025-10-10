package eu.occtet.boc.spdx.service;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.spdx.dao.CodeLocationRepository;
import eu.occtet.boc.spdx.dao.CopyrightRepository;
import eu.occtet.boc.spdx.factory.CodeLocationFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeLocationService {

    private static final Logger log = LogManager.getLogger(CodeLocationService.class);


    @Autowired
    private CodeLocationFactory codeLocationFactory;

    @Autowired
    private CodeLocationRepository codeLocationRepository;

    public CodeLocation findOrCreateCodeLocationWithInventory(String filePath, InventoryItem inventoryItem) {
        return codeLocationFactory.createWithInventory(filePath, inventoryItem);
    }

    public CodeLocation update(CodeLocation codeLocation){
        return codeLocationRepository.save(codeLocation);
    }
}
