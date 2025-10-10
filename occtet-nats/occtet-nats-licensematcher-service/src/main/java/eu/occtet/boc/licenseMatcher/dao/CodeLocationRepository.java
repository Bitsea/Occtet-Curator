package eu.occtet.boc.licenseMatcher.dao;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeLocationRepository extends JpaRepository<CodeLocation, Long> {

    List<CodeLocation> findByFilePath( String filePath);
    List<CodeLocation> findByInventoryItem(InventoryItem item);
}
