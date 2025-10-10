package eu.occtet.bocfrontend.engine;

import eu.occtet.bocfrontend.dao.ScannerInitializerRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.factory.InventoryItemFactory;
import eu.occtet.bocfrontend.factory.ProjectFactory;
import eu.occtet.bocfrontend.factory.ScannerInitializerFactory;
import eu.occtet.bocfrontend.factory.SoftwareComponentFactory;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.core.security.Authenticated;
import io.jmix.core.security.SystemAuthenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureDataJpa
public class ScannerManagerTest {

    @Autowired
    private ScannerManager scannerManager;


    @Autowired
    private DataManager dataManager;

    @Autowired
    private FetchPlans fetchPlans;

    @Autowired
    private ScannerInitializerRepository scannerInitializerRepository;

    @Autowired
    private ScannerInitializerFactory scannerInitializerFactory;

    @Autowired
    private SoftwareComponentFactory softwareComponentFactory;

    @Autowired
    private InventoryItemFactory inventoryItemFactoryFactory;

    @Autowired
    private ProjectFactory projectFactory;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    private static final Logger log = LogManager.getLogger(ScannerManagerTest.class);


    @Authenticated
    private InventoryItem prepare() {

        Project project = projectFactory.create("project1");
        dataManager.save(project);
        SoftwareComponent softwareComponent = softwareComponentFactory.create("component1", "1.0");
        InventoryItem inventoryItem = inventoryItemFactoryFactory.create("Inventory1", softwareComponent, project);
        dataManager.save(inventoryItem);
        return  inventoryItem;
    }


    @Test
    public void testWithDumbScanner() {

        systemAuthenticator.runWithSystem(()-> {

            ScannerInitializer scannerInitializer = scannerInitializerFactory.create(prepare(), "dumb");
            log.debug("Created ScannerInitializer: {}", scannerInitializer);
            scannerManager.enqueueScannerInitializer(scannerInitializer);
            log.debug("Enqueued ScannerInitializer: {}", scannerInitializer);
            assertEquals(1, scannerManager.countWaitingInitializers());

            // process manually once (scheduling is disabled in tests)
            scannerManager.processQueue();

            assertEquals(0, scannerManager.countWaitingInitializers());
        });
    }

}
