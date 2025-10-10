package eu.occtet.boc.ai.copyrightFilter.dao;

import eu.occtet.boc.entity.InformationFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InformationFileRepository extends CrudRepository<InformationFile, UUID> {

    List<InformationFile> findByFileName(String name);
    List<InformationFile> findByContext(String context);
    List<InformationFile> findAll();

}
