package eu.occtet.bocfrontend.view.ortIssue;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.OrtIssue;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.*;

@Route(value = "ortIssue", layout = MainView.class)
@ViewController(id = "ortIssue.list")
@ViewDescriptor(path = "ort-issue-list-view.xml")
@LookupComponent("ortIssueDataGrid")
@DialogMode(width = "64em")
public class OrtIssueListView extends StandardListView<OrtIssue> {
}
