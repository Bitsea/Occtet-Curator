package eu.occtet.bocfrontend.importer;

import eu.occtet.bocfrontend.entity.ImportStatus;
import eu.occtet.bocfrontend.entity.ImportTask;
import eu.occtet.bocfrontend.factory.ImportTaskFactory;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImportManager {


    private static final Logger log = LogManager.getLogger(ImportManager.class);


    // Connect to all available Importer implementations. Small but effective Spring autowire trick :-)
    @Autowired
    private List<Importer> importers;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private ImportTaskFactory importTaskFactory;


    private Importer preselectedImporter;

    /**
     * @return list of available import names (for dropdown when selecting which importer to create)
     */
    public List<Importer> getAvailableImports() {
        log.debug("found {} available imports", importers.size());
        return importers.stream().filter(i -> !i.getName().equals("Dumb")).collect(Collectors.toCollection(ArrayList::new));
    }

    public void preselectNewImport(Importer importer) {
        this.preselectedImporter = importer;
    }

    public Importer getPreselectedImporter() {
        log.debug("preselected import: {}", preselectedImporter);
        return preselectedImporter;
    }

    public void processImport(ImportTask importTask){

        // if a initializer available, process it
        if(importTask != null) {
            boolean res = false;

            try {
                log.debug("processing scannerInitializer from queue: {}", importTask);
                res = preselectedImporter.processTask(importTask);
                log.info("finished processing task for Scanner: {}. Result: {}", importTask.getId(), res);
                importTaskFactory.saveWithFeedBack(importTask, List.of("Finished processing task for Scanner: " + importTask.getImportName() + ". Result: " + res),  res ? ImportStatus.COMPLETED : ImportStatus.STOPPED);
            } catch (Exception e) {
                importTaskFactory.saveWithFeedBack(importTask,List.of("Exception during processing: " + e.getMessage()), ImportStatus.STOPPED);
                log.error("exception during processing of scannerInitializer {}", importTask.getImportName(), e);
            }

            // clear file upload once finished
            clearFileUpload(importTask);
        }
    }


    /**
     * @param name
     * @return the import with given name or null if not found
     */
    public Importer findImportByName(@Nonnull String name) {
        log.debug("looking for import with name {}", name);
        return importers.stream().filter(s -> StringUtils.equals(name, s.getName())).findFirst().orElse(null);
    }

    private void clearFileUpload(ImportTask importTask){
       ImportTask existingSc = dataManager.load(ImportTask.class).id(importTask.getId()).one();
        existingSc.getImportConfiguration()
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
        log.info("cleared file upload for scanner {}", importTask.getId());
    }
}
