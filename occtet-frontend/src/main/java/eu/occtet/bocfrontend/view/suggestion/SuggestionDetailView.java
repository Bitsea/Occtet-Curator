package eu.occtet.bocfrontend.view.suggestion;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Suggestion;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.*;

import java.util.Arrays;


@Route(value = "suggestion/:id", layout = MainView.class)
@ViewController(id = "Suggestion.detail")
@ViewDescriptor(path = "suggestion-detail-view.xml")
@EditedEntityContainer("suggestionDc")
@DialogMode(width = "90%", height = "90%")
public class SuggestionDetailView extends StandardDetailView<Suggestion> {

    @ViewComponent
    private JmixComboBox<String> contextField;

    @Subscribe
    public void onInit(final InitEvent event) {contextField.setItems(
            Arrays.stream(SuggestionEnum.values())
                    .map(SuggestionEnum::getId)
                    .toList()
    );
    }

}
