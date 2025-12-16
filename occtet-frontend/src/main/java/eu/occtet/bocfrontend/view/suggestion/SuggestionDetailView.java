package eu.occtet.bocfrontend.view.suggestion;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Suggestion;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;


@Route(value = "suggestion/:id", layout = MainView.class)
@ViewController(id = "Suggestion.detail")
@ViewDescriptor(path = "suggestion-detail-view.xml")
@EditedEntityContainer("suggestionDc")
public class SuggestionDetailView extends StandardDetailView<Suggestion> {
}
