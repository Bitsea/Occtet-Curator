package eu.occtet.bocfrontend.importer;

import eu.occtet.bocfrontend.entity.ImportStatus;
import eu.occtet.bocfrontend.entity.ImportTask;
import eu.occtet.bocfrontend.service.ImportTaskService;
import eu.occtet.bocfrontend.view.importer.ImporterListView;
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
    private List<Importer> importer;

    @Autowired
    private ImportTaskService importTaskService;


    private Importer preselectedImporter;
    private ImporterListView importerListView;

    /**
     * @return list of available import names (for dropdown when selecting which importer to create)
     */
    public List<Importer> getAvailableImports() {
        log.debug("found {} available imports", importer.size());
        return importer.stream().filter(i -> !i.getName().equals("Dumb")).collect(Collectors.toCollection(ArrayList::new));
    }

    public void preselectNewImport(Importer importer) {
        this.preselectedImporter = importer;
    }

    public Importer getPreselectedImporter() {
        log.debug("preselected import: {}", preselectedImporter);
        return preselectedImporter;
    }


    /**
     * @param name
     * @return the import with given name or null if not found
     */
    public Importer findImportByName(@Nonnull String name) {
        log.debug("looking for scanner with name {}", name);
        return importer.stream().filter(s -> StringUtils.equals(name, s.getName())).findFirst().orElse(null);
    }
}
