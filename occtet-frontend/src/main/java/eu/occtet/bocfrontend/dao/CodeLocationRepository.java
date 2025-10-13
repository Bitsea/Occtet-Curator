package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeLocationRepository  extends JmixDataRepository<CodeLocation, UUID> {
    @Query("SELECT c " + "FROM CodeLocation c " + "WHERE :filePath LIKE CONCAT('%', c.filePath) " + "AND c.inventoryItem.project = :project")
    Optional<CodeLocation> findByFilePathEndingWith(@Param("filePath") String filePath, @Param("project") Project project);

    List<CodeLocation> findByInventoryItem_Project(Project project);
}
