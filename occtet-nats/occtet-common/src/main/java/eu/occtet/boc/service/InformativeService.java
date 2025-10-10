package eu.occtet.boc.service;

import eu.occtet.boc.model.WorkerStatus;

public interface InformativeService {
    int getProgressPercent();

    WorkerStatus getWorkerStatus();

    String getStatusDetails();
}
