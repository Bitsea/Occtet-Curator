package eu.occtet.bocfrontend.engine;


import eu.occtet.bocfrontend.dao.ScannerInitializerRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;
import eu.occtet.bocfrontend.factory.ScannerInitializerFactory;
import eu.occtet.bocfrontend.scanner.Scanner;
import eu.occtet.bocfrontend.service.ScannerInitializerService;
import eu.occtet.bocfrontend.view.scannerInitializer.ScannerInitializerListView;
import io.jmix.core.DataManager;
import io.jmix.core.security.Authenticated;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScannerManager {

    private static final Logger log = LogManager.getLogger(ScannerManager.class);
    private final ScannerInitializerQueue scannerInitializerQueue;

    // Connect to all available Scanner implementations. Small but effective Spring autowire trick :-)
    @Autowired
    private List<Scanner> scanners;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private ScannerInitializerService scannerInitializerService;
    @Autowired
    private ScannerInitializerRepository scannerIntializerRepository;

    @Autowired
    private ScannerInitializerFactory scannerInitializerFactory;

    private String preselectedScanner;
    private ScannerInitializerListView scannerInitializerListView;

    public ScannerManager(ScannerInitializerQueue scannerInitializerQueue) {
        this.scannerInitializerQueue = scannerInitializerQueue;
    }

    public void updateScannerInitializerStatus(ScannerInitializerStatus initialState, ScannerInitializerStatus goalState) {
        scannerIntializerRepository.findByStatus(initialState.getId()).forEach(scanner -> {
            scanner.updateStatus(goalState.getId());
            dataManager.save(scanner);
        });
    }

    /**
     * @return list of available scanner names (for dropdown when selecting which scannerInitializer to create)
     */
    public List<String> getAvailableScanners() {
        log.debug("found {} available scanners", scanners.size());
        return scanners.stream().map(Scanner::getName).filter(name -> !name.equals("Dumb")).collect(Collectors.toList());
    }

    public List<ScannerInitializer> getWaitingAndRunningScanners() {
        List<ScannerInitializer> list = new ArrayList<>();
        list.addAll(scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.IN_PROGRESS));
        list.addAll(scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.WAITING));
        return list;
    }


    public void preselectNewScanner(String scanner) {
        this.preselectedScanner = scanner;
    }

    public String getPreselectedScanner() {
        log.debug("preselected scanner: {}", preselectedScanner);
        return preselectedScanner;
    }

    public List<ScannerInitializer> getStoppedScanners() {
        List<ScannerInitializer> list = new ArrayList<>();
        list.addAll(scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.STOPPED));
        log.debug("found {} stopped scanners", list.size());
        return list;
    }

    /**
     * @param name
     * @return the scanner with given name or null if not found
     */
    public Scanner findScannerByName(@Nonnull String name) {
        log.debug("looking for scanner with name {}", name);
        return scanners.stream().filter(s -> StringUtils.equals(name, s.getName())).findFirst().orElse(null);
    }

    /**
     * Process the given scannerInitializer. This will find the correct Scanner implementation and call it to process
     * the task.
     *
     *scannerInitializerQueue for the scannerInitializer to process
     */
    @Scheduled(cron = "${scannerInitializerQueue.cron}")
    @Authenticated
    public void processQueue() {
        ScannerInitializer existingSc = scannerInitializerQueue.poll();

        // if a initializer available, process it
        if(existingSc != null) {
            boolean res = false;

            try {
                log.debug("processing scannerInitializer from queue: {}", existingSc);
                res = processScannerTask(existingSc);
                log.info("finished processing task for Scanner: {}. Result: {}", existingSc.getId(), res);
                scannerInitializerFactory.saveWithFeedBack(existingSc, List.of("Finished processing task for Scanner: " + existingSc.getScanner() + ". Result: " + res),  res ? ScannerInitializerStatus.COMPLETED : ScannerInitializerStatus.STOPPED);
            } catch (Exception e) {
                scannerInitializerService.updateScannerStatus(ScannerInitializerStatus.STOPPED, existingSc);
                scannerInitializerService.updateScannerFeedback("Exception during processing: " + e.getMessage(), existingSc);
                log.error("exception during processing of scannerInitializer {}", existingSc.getScanner(), e);
            }

            log.debug("Feedback: {}", existingSc.getFeedback());
            // clear file upload once finished
            clearFileUpload(existingSc);
        }
    }

    /**
     * @param scannerInitializer
     * @return true on success, false if scanner not found or failed
     */
    private boolean processScannerTask(
            @Nonnull ScannerInitializer scannerInitializer) {
        // find the scanner for this task by name
        Optional<Scanner> optionalScanner = scanners.stream().filter(
                        a -> StringUtils.equalsIgnoreCase(a.getName(), scannerInitializer.getScanner()))
                .findFirst();

        log.debug("found scanner with name {}: {}", scannerInitializer.getScanner(), optionalScanner.isPresent());

        // if we found a fitting scanner, call it to process the task
        if (optionalScanner.isPresent()) {
            try {
                Scanner scanner = optionalScanner.get();
               return scanner.processTask(scannerInitializer,
                        this::onScannerInitializerCompleted);
            }catch(Exception e){
                log.error("could not process initializer due to exception", e);
                return false;
            }
        }
        // no matching scanner found
        log.error("could not find scanner with name {}", scannerInitializer.getScanner());
        return false;
    }

    private void onScannerInitializerCompleted(ScannerInitializer initializer) {
        scannerInitializerQueue.remove(initializer);
    }

    public void enqueueScannerInitializer(@Nonnull ScannerInitializer scannerInitializer) {
        log.debug("enqueuing scannerInitializer {}", scannerInitializer);
        scannerInitializerQueue.add(scannerInitializer);
    }

    public int countWaitingInitializers() {
        log.debug("waiting initializers in queue: {}", scannerInitializerQueue.size());
        return scannerInitializerQueue.size();
    }


    public void setTaskListView(ScannerInitializerListView scannerInitializerListView) {
        this.scannerInitializerListView = scannerInitializerListView;
    }

    private void clearFileUpload(ScannerInitializer scannerInitializer){
        ScannerInitializer existingSc = dataManager.load(ScannerInitializer.class).id(scannerInitializer.getId()).one();
        existingSc.getScannerConfiguration()
                .stream()
                .filter(config -> config.getUpload() != null)
                .findFirst()
                .ifPresent(config -> {
                    log.debug(config.getUpload() != null ? "upload not null" : "upload null");
                    config.setUpload(null);
                    log.debug(config.getUpload() != null ? "upload not null" : "upload null");
                    dataManager.save(config);
                });
        dataManager.save(existingSc);
        log.info("cleared file upload for scanner {}", scannerInitializer.getId());
    }


}
