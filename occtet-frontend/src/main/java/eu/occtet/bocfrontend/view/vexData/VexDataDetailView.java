package eu.occtet.bocfrontend.view.vexData;

import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;


@Route(value = "vexData/:id", layout = MainView.class)
@ViewController(id = "VexData.detail")
@ViewDescriptor(path = "vex-data-detail-view.xml")
@EditedEntityContainer("vexDataDc")
public class VexDataDetailView extends StandardDetailView<VexData> {

}
