package eu.occtet.bocfrontend.view.ortViolation;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.OrtViolation;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "ortViolation/:id", layout = MainView.class)
@ViewController(id = "OrtViolation.detail")
@ViewDescriptor(path = "ort-violation-view.xml")
@EditedEntityContainer("ortViolationDc")
public class OrtViolationDetailView extends StandardDetailView<OrtViolation> {


}
