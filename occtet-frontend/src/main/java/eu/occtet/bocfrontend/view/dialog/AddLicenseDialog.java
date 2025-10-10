package eu.occtet.bocfrontend.view.dialog;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@ViewController("addLicenseDialog")
@ViewDescriptor("add-license-dialog.xml")
@DialogMode(width = "900px", height = "650px")
public class AddLicenseDialog extends AbstractAddContentDialog<SoftwareComponent> {

    private SoftwareComponent softwareComponent;

    private License license;

    @Autowired
    private LicenseRepository licenseRepository;

    @ViewComponent
    private CollectionContainer<License> licenseDc;

    @ViewComponent
    private TextField searchField;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @ViewComponent
    private DataGrid<License> licensesDataGrid;

    @Override
    @Subscribe("licenseDc")
    public void setAvailableContent(SoftwareComponent softwareComponent){
        this.softwareComponent = softwareComponent;
        licenseDc.setItems(licenseRepository.findAll());
    }

    @Subscribe("licensesDataGrid")
    public void selectAvailableContent(final ItemClickEvent<License> event){license = event.getItem();}

    @Override
    @Subscribe(id = "addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<License> licenses = new ArrayList<>(licensesDataGrid.getSelectedItems());

        if(event != null & licenses != null){
            for(License license : licenses){
                if(!this.softwareComponent.getLicenses().contains(license)){
                    this.softwareComponent.getLicenses().add(license);
                }
            }
            softwareComponentRepository.save(this.softwareComponent);
            close(StandardOutcome.CLOSE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){
            List<License> list = licenseRepository.findByLicenseName(searchWord);
            licenseDc.setItems(list);
        }else{
            licenseDc.setItems(licenseRepository.findAll());
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event){cancelButton(event);}
}
