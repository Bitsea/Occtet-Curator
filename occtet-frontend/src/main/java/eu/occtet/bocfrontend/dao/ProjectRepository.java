package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;


public interface ProjectRepository extends JmixDataRepository<Project, UUID> {
    List<Project> findAll();
}
