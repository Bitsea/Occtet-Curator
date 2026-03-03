package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.OrtViolation;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;

public interface OrtViolationRepository extends JmixDataRepository<OrtViolation, Long> {

    List<OrtViolation> findAll();
    List<OrtViolation> findByProjectId(Long projectId);
}
