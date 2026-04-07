package eu.occtet.bocfrontend.view.ortViolation;


import com.vaadin.flow.component.AbstractField;

import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.OrtViolation;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;


@Route(value = "ortViolation/:id", layout = MainView.class)
@ViewController(id = "OrtViolation.detail")
@ViewDescriptor(path = "ort-violation-view.xml")
@EditedEntityContainer("ortViolationDc")
public class OrtViolationDetailView extends StandardDetailView<OrtViolation> {

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
