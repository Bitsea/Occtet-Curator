package eu.occtet.bocfrontend.view.copyright;

import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "copyrights/:id", layout = MainView.class)
@ViewController(id = "Copyright.detail")
@ViewDescriptor(path = "copyright-detail-view.xml")
@EditedEntityContainer("copyrightDc")
public class CopyrightDetailView extends StandardDetailView<Copyright> {
}