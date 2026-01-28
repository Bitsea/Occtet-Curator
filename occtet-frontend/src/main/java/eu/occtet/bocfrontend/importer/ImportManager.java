package eu.occtet.bocfrontend.importer;

import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.TaskStatus;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.service.CuratorTaskService;
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
    private final CuratorTaskService curatorTaskService;


    // Connect to all available Importer implementations. Small but effective Spring autowire trick :-)
    @Autowired
    private List<Importer> importers;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CuratorTaskFactory curatorTaskFactory;

    public ImportManager(CuratorTaskService curatorTaskService) {
        this.curatorTaskService = curatorTaskService;
    }


    /**
     * @return list of available import names (for dropdown when selecting which importer to create)
     */
    public List<Importer> getAvailableImports() {
        log.debug("found {} available imports", importers.size());
        return importers.stream().filter(i -> !i.getName().equals("Dumb")).collect(Collectors.toCollection(ArrayList::new));
    }


    public void startImport(Importer importer, CuratorTask curatorTask) {

        // if a initializer available, process it
        if (curatorTask != null) {
            boolean res = false;

            try {

                res = importer.prepareAndStartTask(curatorTask);
                log.info("started task {}, importer {}. Result: {}", curatorTask.getId(), importer.getName(), res);
                if(!res) {
                    curatorTask.setStatus(TaskStatus.CANCELLED);
                }
                dataManager.save(curatorTask);
            } catch (Exception e) {
                curatorTaskService.saveWithFeedBack(curatorTask, List.of("Exception during processing: " + e.getMessage()), TaskStatus.CANCELLED);
                log.error("exception during start of scannerInitializer {}", curatorTask.getTaskName(), e);
            }

            // clear file upload once finished
            clearFileUpload(curatorTask);
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

    private void clearFileUpload(CuratorTask curatorTask) {
        curatorTask.getTaskConfiguration().stream().filter(config -> config.getUpload() != null)
                .findFirst().ifPresent(config -> {
                    config.setUpload(null);
                    dataManager.save(curatorTask);
                    log.info("cleared file upload for scanner {}", curatorTask.getId());
                });

    }
}
