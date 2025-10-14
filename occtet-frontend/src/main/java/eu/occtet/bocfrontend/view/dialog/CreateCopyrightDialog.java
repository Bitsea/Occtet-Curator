package eu.occtet.bocfrontend.view.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.CodeLocationRepository;
import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.service.CopyrightService;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


@ViewController("createCopyrightDialog")
@ViewDescriptor("create-copyright-dialog.xml")
@DialogMode(width = "900px", height = "650px")
public class CreateCopyrightDialog extends AbstractCreateContentDialog<InventoryItem>{

    private static final Logger log = LogManager.getLogger(CreateCopyrightDialog.class);

    private InventoryItem inventoryItem;

    @ViewComponent
    private TextField copyrightNameField;

    @ViewComponent
    private JmixComboBox<CodeLocation> copyrightFilePathComboBox;

    @ViewComponent
    private Checkbox isGarbageField;

    @ViewComponent
    private Checkbox isCuratedField;

    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private Notifications notifications;
    @Autowired
    private CodeLocationRepository codeLocationRepository;


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){
        isGarbageField.setValue(false);
        isCuratedField.setValue(false);

        copyrightFilePathComboBox.setItems(codeLocationRepository.findByInventoryItem_Project(inventoryItem.getProject()));
        copyrightFilePathComboBox.setItemLabelGenerator(CodeLocation::getFilePath);
    }

    @Override
    public void setAvailableContent(InventoryItem content) {this.inventoryItem = content;}

    @Override
    @Subscribe("addCopyrightButton")
    public void addContentButton(ClickEvent<Button> event) {

        String copyrigthName = copyrightNameField.getValue();
        CodeLocation location = copyrightFilePathComboBox.getValue();

        if(checkInput(copyrigthName,location)){

            Copyright copyright = copyrightService.createCopyright(copyrigthName,location,
                    isCuratedField.getValue(),isGarbageField.getValue());

            this.inventoryItem.getCopyrights().add(copyright);
            dataManager.save(inventoryItem);
            log.debug("Created and added copyright {} to inventoryItem",copyright.getCopyrightText());
            close(StandardOutcome.CLOSE);
        }else{
            notifications.create("Something went wrong, please check your input")
                    .withDuration(3000)
                    .withPosition(Notification.Position.TOP_CENTER)
                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
                    .show();
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelCopyright(ClickEvent<Button> event){cancelButton(event);}

    private boolean checkInput(String name, CodeLocation codeLocation){

        if(!name.isEmpty() && codeLocation != null){
            return true;
        }
        return false;
    }

}
