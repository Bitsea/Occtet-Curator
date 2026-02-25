package eu.occtet.bocfrontend.view.ortIssue;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.OrtIssue;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "ortIssue/:id", layout = MainView.class)
@ViewController(id = "OrtIssue.detail")
@ViewDescriptor(path = "ort-issue-view.xml")
@EditedEntityContainer("ortIssueDc")
public class OrtIssueDetailView extends StandardDetailView<OrtIssue> {
}
