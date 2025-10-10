package eu.occtet.bocfrontend.engine;

import eu.occtet.bocfrontend.dao.ScannerInitializerRepository;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;
import io.jmix.core.DataManager;
import io.jmix.core.security.Authenticated;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * simple queue implementation for scannerInitializer. This does not persist scanners and thus needs to be replaced.
 */
@Service
public class ScannerInitializerQueue implements SimpleQueue<ScannerInitializer> {

    private static final Logger log = LogManager.getLogger(ScannerInitializerQueue.class);

    @Autowired
    private ScannerInitializerRepository scannerInitializerRepository;

    @Autowired
    private DataManager dataManager;


    /**
     * saves scannerInitializer with status waiting
     * @param scannerInitializer
     */
    @Override
    @Authenticated // authenticates the entire method
    public void add(@Nonnull ScannerInitializer scannerInitializer) {
        scannerInitializer.updateStatus(ScannerInitializerStatus.WAITING.getId());
        scannerInitializerRepository.save(scannerInitializer);
        log.debug("saved scannerInitializer: {}", scannerInitializer);

    }

    @Override
    @Authenticated // authenticates the entire method
    public ScannerInitializer poll() {
        UUID scannerInitializerId = pollInternal();
        if(scannerInitializerId==null) return  null;
        log.debug("scanner load fetchplan {}",dataManager.load(ScannerInitializer.class).id(scannerInitializerId).joinTransaction(false).fetchPlan("full").one().getScanner() );
        return dataManager.load(ScannerInitializer.class).id(scannerInitializerId).joinTransaction(false).fetchPlan("full").one();
    }

    /**
     *
     * @return id of scannerIntializer or null if nothing found
     */
    @Transactional
    private UUID pollInternal() {
        log.trace("polling scannerInitializers queue which has  {} waiting and a total of {}",
                scannerInitializerRepository.countByStatus(ScannerInitializerStatus.WAITING.getId()),
                scannerInitializerRepository.count());
        List<ScannerInitializer> scannerInitializers = scannerInitializerRepository
                .findByStatus(ScannerInitializerStatus.WAITING.getId());

        // found something
        for (ScannerInitializer initializer : scannerInitializers) {
            // set status to in progress
            initializer.updateStatus(ScannerInitializerStatus.IN_PROGRESS.getId());
            scannerInitializerRepository.save(initializer);
            return initializer.getId();
        }
        return  null;
    }



    @Override
    @Authenticated // authenticates the entire method
    public int size() {
        return (int) scannerInitializerRepository.countByStatus(ScannerInitializerStatus.WAITING.getId());
    }

    @Override
    @Authenticated
    public void remove(ScannerInitializer scannerInitializerToRemove) {
        Optional<ScannerInitializer> scannerInitializer = scannerInitializerRepository.findById(scannerInitializerToRemove.getId());
        if(scannerInitializer.isPresent()) {
            scannerInitializer.get().updateStatus(ScannerInitializerStatus.COMPLETED.getId());
            dataManager.save(scannerInitializer.get());
        }
    }

    @Override
    public void clear() {
        scannerInitializerRepository.deleteAll(scannerInitializerRepository.findByStatus(ScannerInitializerStatus.WAITING.getId()));
    }
}
