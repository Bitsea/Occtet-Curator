package eu.occtet.bocfrontend.view.ortIssue;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.OrtIssue;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "ortIssue/:id", layout = MainView.class)
@ViewController(id = "OrtIssue.detail")
@ViewDescriptor(path = "ort-issue-view.xml")
@EditedEntityContainer("ortIssueDc")
public class OrtIssueDetailView extends StandardDetailView<OrtIssue> {

    private static final Logger log = LogManager.getLogger(OrtIssueDetailView.class);

    @ViewComponent
    private JmixComboBox<Project> projectField;
    @ViewComponent
    private JmixComboBox<InventoryItem> inventoryItemField;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;


    @Subscribe
    public void onInit(InitEvent event){
        projectField.setItems(projectRepository.findAll());
        projectField.setItemLabelGenerator(p -> p.getProjectName() + " - " +p.getVersion());
    }

    @Subscribe(id = "projectField")
    public void chooseProject(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        Project project = event.getValue();
        if(project != null){
            inventoryItemField.setItems(inventoryItemRepository.findByProject(project));
            inventoryItemField.setItemLabelGenerator(InventoryItem::getInventoryName);
        }
    }
}
