package eu.occtet.bocfrontend.view.ortViolation;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.OrtViolation;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "ortViolation", layout = MainView.class)
@ViewController(id = "ortViolation.list")
@ViewDescriptor(path = "ort-violation-list-view.xml")
@LookupComponent("ortViolationDataGrid")
@DialogMode(width = "64em")
public class OrtViolationListView extends StandardListView<OrtViolation> {

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private HorizontalLayout filterBox;

    @ViewComponent
    private CollectionLoader<OrtViolation> ortViolationDl;

    @Autowired
    private ProjectRepository projectRepository;

    @Subscribe
    public void onInit(InitEvent event){
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
    }

    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        if(event != null){
            ortViolationDl.setParameter("project",event.getValue());
            ortViolationDl.load();
            filterBox.setVisible(true);
        }
    }
}
