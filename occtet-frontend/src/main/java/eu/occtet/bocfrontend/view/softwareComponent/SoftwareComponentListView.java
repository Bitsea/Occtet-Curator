package eu.occtet.bocfrontend.view.softwareComponent;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.SoftwareComponentService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@Route(value = "software-components", layout = MainView.class)
@ViewController(id = "SoftwareComponent.list")
@ViewDescriptor(path = "software-component-list-view.xml")
@LookupComponent("softwareComponentsDataGrid")
@DialogMode(width = "64em")
public class SoftwareComponentListView extends StandardListView<SoftwareComponent> {

    private final List<String> valuesOfBoolean = List.of("True","False");
    private List<SoftwareComponent> softwareComponentsList;

    @ViewComponent
    private JmixComboBox<License> licenseComboBox;

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private CollectionContainer<SoftwareComponent> softwareComponentsDc;

    @ViewComponent
    private CollectionLoader<SoftwareComponent> softwareComponentsDl;

    @ViewComponent
    private Accordion projectAccordion;

    @ViewComponent
    private Accordion attributesAccordion;

    @ViewComponent
    private JmixComboBox<String> curatedComboBox;

    @ViewComponent
    private JmixComboBox<String> cveComboBox;

    @ViewComponent
    private JmixComboBox<String> vulnerabilityComboBox;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SoftwareComponentService softwareComponentService;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Subscribe
    public void onInit(InitEvent event){
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);

        licenseComboBox.setItems(licenseRepository.findAll());
        licenseComboBox.setItemLabelGenerator(License::getLicenseType);

        curatedComboBox.setItems(valuesOfBoolean);
        cveComboBox.setItems(softwareComponentService.getAllCVEFoundInSoftwareComponents(softwareComponentRepository.findAll()));
        vulnerabilityComboBox.setItems(valuesOfBoolean);

        projectAccordion.close();
        attributesAccordion.close();

        softwareComponentsList = softwareComponentRepository.findAll();
    }

    @Subscribe("projectComboBox")
    public void showSoftwareComponentFromProject(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>,
            Project> event){
        applyAllFilters();
    }

    @Subscribe("licenseComboBox")
    public void showSoftwareComponentsFromLicenses(final AbstractField.ComponentValueChangeEvent<JmixComboBox<License>,
            License> event){
        applyAllFilters();

    }

    @Subscribe("curatedComboBox")
    public void findCopyrightsByCurated(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,
            String> event){
        applyAllFilters();
    }

    @Subscribe("vulnerabilityComboBox")
    public void onVulnerabilityComboBoxValueChangefinal(AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,
    String> event){
        applyAllFilters();
    }

    @Subscribe("cveComboBox")
    public void onCveComboBoxValueChangefinal(AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,
            String> event){
        applyAllFilters();
    }

    @Subscribe("searchButton")
    public void searchSoftwareComponent(ClickEvent<Button> event){

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){

            List<SoftwareComponent> scList = softwareComponentsDc.getItems();
            List<SoftwareComponent> searchSc = new ArrayList<>();

            for(SoftwareComponent sc : scList){
                if(sc.getName().toUpperCase().contains(searchWord.toUpperCase()))
                    searchSc.add(sc);
            }
            softwareComponentsDc.setItems(searchSc);
        }else if(searchWord.isEmpty() && event != null){
            softwareComponentsDl.load();
        }
    }

    private void applyAllFilters(){
        Project project = projectComboBox.getValue();
        License license = licenseComboBox.getValue();
        Boolean curated = curatedComboBox.getValue() != null ? Boolean.parseBoolean(curatedComboBox.getValue()) : null;
        String cve = cveComboBox.getValue();
        Boolean isVulnerable = vulnerabilityComboBox.getValue() != null ? Boolean.parseBoolean(vulnerabilityComboBox.getValue()) : null;

        List<SoftwareComponent> filteredSoftwareComponents = new ArrayList<>(softwareComponentsList);

        if (project != null){
            filteredSoftwareComponents.retainAll(softwareComponentService.findSoftwareComponentsByProject(project));
        }
        if (license != null){
            filteredSoftwareComponents.retainAll(softwareComponentService.findSoftwareComponentsByLicense(license));
        }
        if(curated != null){
            filteredSoftwareComponents.retainAll(softwareComponentService.findSoftwareComponentsByCurated(curated));
        }
//        if (cve != null){ TODO check ticket 978
//            filteredSoftwareComponents.retainAll(softwareComponentRepository.searchSoftwareComponentsByCveContainingIgnoreCase(cve));
//        }
        if (isVulnerable != null){
            filteredSoftwareComponents.retainAll(softwareComponentService.findSoftwareComponentsByIsVulnerable(isVulnerable));
        }


        softwareComponentsDc.setItems(filteredSoftwareComponents);
    }
}