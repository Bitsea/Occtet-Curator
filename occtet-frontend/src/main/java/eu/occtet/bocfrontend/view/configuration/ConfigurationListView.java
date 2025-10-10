package eu.occtet.bocfrontend.view.configuration;

import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.*;

@Route(value = "configurations", layout = MainView.class)
@ViewController("Configuration.list")
@ViewDescriptor("configuration-list-view.xml")
@LookupComponent("configurationsDataGrid")
@DialogMode(width = "64em")
public class ConfigurationListView extends StandardListView<Configuration> {
}