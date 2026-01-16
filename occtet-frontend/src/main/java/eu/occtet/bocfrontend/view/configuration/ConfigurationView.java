package eu.occtet.bocfrontend.view.configuration;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.Messages;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "configuration-view", layout = MainView.class)
@ViewController(id = "ConfigurationView")
@ViewDescriptor(path = "configuration-view.xml")
public class ConfigurationView extends StandardView {

    @Autowired
    private Messages messages;
    @Autowired
    private AppSettings appSettings;

}