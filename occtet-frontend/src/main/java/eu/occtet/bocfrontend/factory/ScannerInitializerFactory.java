package eu.occtet.bocfrontend.factory;


import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScannerInitializerFactory {

    @Autowired
    private DataManager dataManager;

    /**
     * create ScannerInitializer entity for given softwareComponent and scanner
     * @param inventoryItem the origin inventory item which will be used as root inventory item
     * @param scanner name of the scanner to use for scanning this softwareComponent.
     * @return the persisted scannerInitializer entity
     */
    public ScannerInitializer create(@Nonnull InventoryItem inventoryItem, @Nonnull String scanner) {
        ScannerInitializer scannerTask = dataManager.create(ScannerInitializer.class);
        scannerTask.setInventoryItem(inventoryItem);
        scannerTask.setScanner(scanner);

        return dataManager.save(scannerTask);
    }

    public ScannerInitializer saveWithFeedBack(ScannerInitializer scannerInitializer, List<String> feedback, ScannerInitializerStatus status){
        List<String> newFeedbacks= new ArrayList<>();
        List<String> oldFeedbacks= scannerInitializer.getFeedback(); // get preexisting feedbacks
        if (oldFeedbacks!=null && !oldFeedbacks.isEmpty()) newFeedbacks.addAll(oldFeedbacks);

        // Add new feedback
        newFeedbacks.addAll(feedback);

        scannerInitializer.setFeedback(newFeedbacks);
        scannerInitializer.setStatus(status.getId());
        return dataManager.save(scannerInitializer);
    }
}
