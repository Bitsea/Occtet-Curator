package eu.occtet.bocfrontend.view.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.LicenseService;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


@ViewController("createLicenseDialog")
@ViewDescriptor("create-license-dialog.xml")
@DialogMode(width = "900px", height = "650px")
public class CreateLicenseDialog extends AbstractCreateContentDialog<SoftwareComponent>{

    private static final Logger log = LogManager.getLogger(CreateLicenseDialog.class);

    private SoftwareComponent softwareComponent;

    @ViewComponent
    private TextField licenseNameField;

    @ViewComponent
    private TextField licenseTypeField;

    @ViewComponent
    private TextField detailsUrlField;

    @ViewComponent
    private TextField priorityField;

    @ViewComponent
    private Checkbox isSpdxField;

    @ViewComponent
    private Checkbox isCuratedField;

    @ViewComponent
    private Checkbox isModifiedField;

    @ViewComponent
    private TextArea licenseTextField;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){
        isCuratedField.setValue(false);
        isModifiedField.setValue(false);
        isSpdxField.setValue(false);
    }

    @Override
    public void setAvailableContent(SoftwareComponent content) {this.softwareComponent = content;}

    @Override
    @Subscribe("addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {

        String priority = priorityField.getValue();
        String licenseType = licenseTypeField.getValue();
        String licenseText = licenseTextField.getValue();
        String licenseName = licenseNameField.getValue();
        String detailsUrl = detailsUrlField.getValue();

        if(checkInput(priority,licenseType,licenseText,licenseName,detailsUrl)){

            License license = licenseService.createLicense(Integer.valueOf(priority),licenseType,licenseText,
                    licenseName,detailsUrl,isModifiedField.getValue(),isCuratedField.getValue(),isSpdxField.getValue());
            softwareComponent.getLicenses().add(license);
            dataManager.save(softwareComponent);
            log.debug("Created and added license {} to softwareComponent",license.getLicenseName());
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
    public void cancelLicense(ClickEvent<Button> event){cancelButton(event);}

    private boolean checkInput(String priority,String licenseType,String licenseText,String licenseName,String detailsUrl ){

        if(!priority.isEmpty() && !licenseType.isEmpty() && !licenseText.isEmpty() && !licenseName.isEmpty() && !detailsUrl.isEmpty()){
            return true;
        }
        return false;
    }
}
