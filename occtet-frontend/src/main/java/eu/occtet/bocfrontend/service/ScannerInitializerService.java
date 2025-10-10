package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.dao.ScannerInitializerRepository;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScannerInitializerService {

    @Autowired
    private ScannerInitializerRepository scannerInitializerRepository;

    @Autowired
    private DataManager dataManager;

    public List<ScannerInitializer> getScannerByStatus(ScannerInitializerStatus status){
        return scannerInitializerRepository.findByStatus(status.getId());
    }

    public long countScannerByStatus(ScannerInitializerStatus status){
        return scannerInitializerRepository.countByStatus(status.getId());
    }

    public void updateScannerFeedback(String feedback, ScannerInitializer scannerInitializer){
        if (scannerInitializer.getFeedback() == null) {
            scannerInitializer.setFeedback(new ArrayList<>());
        }
        scannerInitializer.getFeedback().add(feedback);
        dataManager.save(scannerInitializer);
    }

    public void updateScannerStatus(ScannerInitializerStatus status, ScannerInitializer scannerInitializer){
        scannerInitializer.updateStatus(status.getId());
        dataManager.save(scannerInitializer);
    }

}
