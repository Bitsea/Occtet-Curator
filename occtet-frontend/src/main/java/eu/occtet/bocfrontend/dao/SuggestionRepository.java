package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.Suggestion;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;



public interface SuggestionRepository extends JmixDataRepository<Suggestion, Long> {
    List<Suggestion> findByContext(String context);
}
