package eu.occtet.boc.spdx.dao;



import eu.occtet.boc.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByProjectName(String projectName);
    List<Project> findAll();
    Optional<Project> findById(UUID uid);
}
