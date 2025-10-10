package eu.occtet.bocfrontend.view.license;

import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "licenses/:id", layout = MainView.class)
@ViewController(id = "License.detail")
@ViewDescriptor(path = "license-detail-view.xml")
@EditedEntityContainer("licenseDc")
public class LicenseDetailView extends StandardDetailView<License> {
}