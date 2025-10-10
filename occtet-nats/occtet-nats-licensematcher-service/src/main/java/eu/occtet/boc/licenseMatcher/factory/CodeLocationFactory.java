package eu.occtet.boc.licenseMatcher.factory;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.licenseMatcher.dao.CodeLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CodeLocationFactory {

    private static final Logger log = LoggerFactory.getLogger(CodeLocationFactory.class);


    @Autowired
    private CodeLocationRepository codeLocationRepository;

    public CodeLocation createCodeLocationWithLineNumbers(String filePath,
                               Integer lineNumberOne, Integer lineNumberTwo
    ) {
         return codeLocationRepository.save(new CodeLocation(filePath,  lineNumberOne,
                 lineNumberTwo));
    }

    public CodeLocation create(String filePath) {
        log.debug("Creating CodeLocation with filePath: {}", filePath);
        return codeLocationRepository.save(new CodeLocation(filePath));
    }


    public CodeLocation createWithInventory(String filePath, InventoryItem originInventoryItem) {
        log.debug("Creating CodeLocation with filePath: {}", filePath);
        return codeLocationRepository.save(new CodeLocation( originInventoryItem, filePath));
    }
}
