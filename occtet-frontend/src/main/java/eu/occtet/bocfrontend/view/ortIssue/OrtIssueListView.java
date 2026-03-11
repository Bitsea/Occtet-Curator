package eu.occtet.bocfrontend.view.ortIssue;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.OrtIssueRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.OrtIssue;
import eu.occtet.bocfrontend.entity.OrtViolation;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "ortIssue", layout = MainView.class)
@ViewController(id = "ortIssue.list")
@ViewDescriptor(path = "ort-issue-list-view.xml")
@LookupComponent("ortIssueDataGrid")
@DialogMode(width = "64em")
public class OrtIssueListView extends StandardListView<OrtIssue> {

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private CollectionContainer<OrtIssue> ortIssueDc;

    @ViewComponent
    private HorizontalLayout filterBox;

    @Autowired
    private ProjectRepository projectRepository;

    @ViewComponent
    private CollectionLoader<OrtIssue> ortIssueDl;

    @Subscribe
    public void onInit(InitEvent event){
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
    }


    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        if(event != null){
            ortIssueDl.setParameter("project",event.getValue());
            ortIssueDl.load();
            filterBox.setVisible(true);
        }
    }
}
