package eu.occtet.bocfrontend.view.help;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "help-view", layout = MainView.class)
@ViewController(id = "HelpView")
@ViewDescriptor(path = "help-view.xml")
public class HelpView extends StandardView {
}