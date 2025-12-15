package eu.occtet.bocfrontend.view.suggestion;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Suggestion;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.*;


@Route(value = "suggestion-components", layout = MainView.class)
@ViewController(id = "Suggestion.list")
@ViewDescriptor(path = "suggestion-list-view.xml")
@LookupComponent("suggestionDataGrid")
@DialogMode(width = "64em")
public class SuggestionListView extends StandardListView<Suggestion> {

}
