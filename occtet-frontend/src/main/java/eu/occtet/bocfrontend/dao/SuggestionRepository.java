package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.Suggestion;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;

public interface SuggestionRepository extends JmixDataRepository<Suggestion, UUID> {
    List<Suggestion> findByContext(String context);
}
