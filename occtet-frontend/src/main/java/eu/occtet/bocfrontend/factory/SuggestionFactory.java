package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.Suggestion;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SuggestionFactory {

    @Autowired
    protected DataManager dataManager;

    public Suggestion create(String context, String sentence) {
        Suggestion suggestion = dataManager.create(Suggestion.class);
        suggestion.setContext(context);
        suggestion.setSentence(sentence);
        return dataManager.save(suggestion);
    }
}
