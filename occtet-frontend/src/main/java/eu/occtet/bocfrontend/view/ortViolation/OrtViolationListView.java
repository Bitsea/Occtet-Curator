package eu.occtet.bocfrontend.view.ortViolation;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.OrtViolation;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.*;

@Route(value = "ortViolation", layout = MainView.class)
@ViewController(id = "ortViolation.list")
@ViewDescriptor(path = "ort-violation-list-view.xml")
@LookupComponent("ortViolationDataGrid")
@DialogMode(width = "64em")
public class OrtViolationListView extends StandardListView<OrtViolation> {

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        if(event != null){
            inventoryItemsDl.setParameter("project",event.getValue());
            inventoryItemsDl.load();
            filterBox.setVisible(true);
        }
    }
}
