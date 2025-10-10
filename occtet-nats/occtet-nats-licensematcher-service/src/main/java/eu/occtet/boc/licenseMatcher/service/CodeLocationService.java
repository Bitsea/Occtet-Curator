package eu.occtet.boc.licenseMatcher.service;

import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.licenseMatcher.dao.CodeLocationRepository;
import eu.occtet.boc.licenseMatcher.factory.CodeLocationFactory;
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


    public CodeLocation findOrCreateCodeLocation(String filePath) {
        List<CodeLocation> codeLocation = codeLocationRepository.findByFilePath(filePath);

        if (!codeLocation.isEmpty() && codeLocation.getFirst() != null) {
            log.debug("Found existing CodeLocation for filePath: {}", filePath);
            return codeLocation.getFirst();
        } else {
            return codeLocationFactory.create(filePath);
        }
    }

    public CodeLocation findOrCreateCodeLocationByFileName(String filePath) {
        List<CodeLocation> codeLocation = codeLocationRepository.findByFilePath(filePath);

        if (!codeLocation.isEmpty() && codeLocation.getFirst() != null) {
            return codeLocationRepository.save(codeLocation.getFirst());
        } else {
            return codeLocationFactory.create(filePath);
        }
    }

    public CodeLocation createCodeLocationWithLineNumber(String filePath,
                                                         Integer lineNumberOne, Integer lineNumberTwo
    ) {
        return codeLocationFactory.createCodeLocationWithLineNumbers(filePath, lineNumberOne,
                lineNumberTwo);
    }

    public CodeLocation createCodeLocation(String filePath
    ) {
        return codeLocationFactory.create(filePath);
    }

    public void update(CodeLocation codeLocation){
        codeLocationRepository.save(codeLocation);
    }

    public CodeLocation findOrCreateCodeLocationWithInventory(String filePath, InventoryItem inventoryItem) {
        return codeLocationFactory.createWithInventory(filePath, inventoryItem);
    }
}
