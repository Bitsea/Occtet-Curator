package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.DBLog;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;



public interface DBLogRepository extends JmixDataRepository<DBLog, UUID> {

    List<DBLog> findAll();

}
