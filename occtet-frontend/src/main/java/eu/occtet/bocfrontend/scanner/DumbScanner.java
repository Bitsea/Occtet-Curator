package eu.occtet.bocfrontend.scanner;


import eu.occtet.bocfrontend.entity.ScannerInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * scanner which is so dumb that it finds nothing.
 * This is a proof of concept class.
 */
@Service
public class DumbScanner extends Scanner {
    public DumbScanner() {
        super("Dumb");
    }

    private static final Logger log = LogManager.getLogger(DumbScanner.class);


    @Override
    public boolean processTask(@Nonnull ScannerInitializer scannerInitializer,
                               @Nonnull Consumer<ScannerInitializer> completionConsumer) {
        // find nothing, don't call findingConsumer, just finish.
        log.debug("DumbScanner: doing nothing for inventory item {}", scannerInitializer.getInventoryItem().getInventoryName());
        completionConsumer.accept(scannerInitializer);
        return true;
    }

}
