package eu.occtet.boc.fossreport.service;

import eu.occtet.boc.entity.ScannerInitializer;
import eu.occtet.boc.fossreport.dao.ScannerInitializerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;


@Service
public class ScannerInitializerService {

    @Autowired
    private ScannerInitializerRepository scannerInitializerRepository;

    public void updateScannerFeedback(ScannerInitializer scannerInitializer, String msg){
        if (scannerInitializer == null) return;
        if (scannerInitializer.getFeedback() == null)
            scannerInitializer.setFeedback(new ArrayList<>());

        scannerInitializer.getFeedback().add(msg);
        scannerInitializerRepository.save(scannerInitializer);
    }
}
