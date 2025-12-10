/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.view.audit.fragment;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.occtet.bocfrontend.dao.*;
import eu.occtet.bocfrontend.dto.AuditCopyrightDTO;
import eu.occtet.bocfrontend.dto.AuditLicenseDTO;
import eu.occtet.bocfrontend.dto.AuditVulnerabilityDTO;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.view.audit.AuditView;
import eu.occtet.bocfrontend.view.dialog.OverviewContentInfoDialog;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;


@FragmentDescriptor("OverviewProjectTabFragment.xml")
public class OverviewProjectTabFragment extends Fragment<VerticalLayout>{

    private static final Logger log = LogManager.getLogger(OverviewProjectTabFragment.class);

    private Project project;
    private View<?> hostView;

    @ViewComponent
    private AccordionPanel copyrightAccordion;

    @ViewComponent
    private AccordionPanel licensesAccordion;

    @ViewComponent
    private AccordionPanel vulnerabilityAccordion;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<AuditCopyrightDTO> auditCopyrightDc;

    @ViewComponent
    private CollectionContainer<AuditLicenseDTO> auditLicensesDc;

    @ViewComponent
    private CollectionContainer<AuditVulnerabilityDTO> auditVulnerabilitiesDc;

    @ViewComponent
    private DataGrid<AuditLicenseDTO> auditLicensesDataGrid;

    @ViewComponent
    private DataGrid<AuditVulnerabilityDTO> auditVulnerabilityDataGrid;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private CodeLocationRepository codeLocationRepository;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Notifications notifications;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private DataManager dataManager;

    private InventoryItem item;


    public void setDefaultAccordionValues(){
        copyrightAccordion.setSummaryText("Copyrights (0)");
        licensesAccordion.setSummaryText("Licenses (0)");
        vulnerabilityAccordion.setSummaryText("Vulnerabilities (0)");
    }

    public void setProjectOverview(@Nonnull Project project){
        this.project = dataContext.merge(project);
        List<InventoryItem> items = inventoryItemRepository.findByProject(project);
        List<CodeLocation> codeLocations = new ArrayList<>();
        List<SoftwareComponent> components = new ArrayList<>();
        if(items != null){
            for(InventoryItem item : items){
                if(item.getSoftwareComponent() != null){
                    components.add(item.getSoftwareComponent());
                }
                codeLocations.addAll(codeLocationRepository.findByInventoryItem(item));
            }
        }
        setAllProjectInformation(this.project);
        addInfoButton();
    }

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
    }

    private void setAllProjectInformation(Project project){
        //setCopyrights(softwareComponents,locations);
        setLicenses(project);
        setVulnerabilities(project);
    }

    private void setCopyrights(List<SoftwareComponent> softwareComponents, List<CodeLocation> locations){

        List<AuditCopyrightDTO> auditCopyrightDTOs = new ArrayList<>();
        Set<Copyright> allCopyrights = new HashSet<>();
        Set<Copyright> filteredCopyrights = new HashSet<>();

        allCopyrights.addAll(softwareComponents.stream().map(SoftwareComponent::getCopyrights)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toSet()));

        filteredCopyrights.addAll(allCopyrights.stream()
                .collect(Collectors.toMap(
                        Copyright::getCopyrightText,
                        copyright->copyright,
                        (c1, c2) -> c1))
                .values());

        for(Copyright copyright : filteredCopyrights){
            int count = 0;
            for(CodeLocation location : locations){
                List<Copyright> copyrightsFromLocation = location.getCopyrights();
                if(copyrightsFromLocation != null){
                   for(Copyright copyright1 : copyrightsFromLocation){
                       if(copyright.getCopyrightText().equals(copyright1.getCopyrightText())){
                           count++;
                       }
                   }
                }
            }
            auditCopyrightDTOs.add(new AuditCopyrightDTO(copyright.getCopyrightText(),count));
        }

        auditCopyrightDc.setItems(auditCopyrightDTOs);
        copyrightAccordion.setSummaryText("Copyrights ("+auditCopyrightDTOs.size()+")");

    }

    private void setLicenses(Project project){

        List<AuditLicenseDTO> licensesDTOs = new ArrayList<>();

        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query("""
                            select l.id as licenseId, count(s) as countSC
                            from InventoryItem i
                            join i.softwareComponent s
                            join s.licenses l
                            join i.project p
                            where p.id = :project_id
                            group by l.id
                       """)
                        .setParameter("project_id",project.getId()))
                .addProperty("licenseId")
                .addProperty("countSC");


        List<KeyValueEntity> values = dataManager.loadValues(context);
        values.forEach(s -> {
            License license = licenseRepository.findLicenseById(s.getValue("licenseId"));
            Long value = s.getValue("countSC");
            licensesDTOs.add(new AuditLicenseDTO(license.getLicenseName(),value.intValue()));
        });
        auditLicensesDc.setItems(licensesDTOs);
        licensesAccordion.setSummaryText("Licenses ("+licensesDTOs.size()+")");
    }

    private void setVulnerabilities(Project project){

        List<AuditVulnerabilityDTO> vulnerabilityDTOs = new ArrayList<>();

        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query("""
                            select v.id as vulnerabilityId, count(s) as countV
                            from InventoryItem i
                            join i.softwareComponent s
                            join s.vulnerabilites v
                            join i.project p
                            where p.id = :project_id
                            group by v.id
                       """)
                        .setParameter("project_id",project.getId()))
                .addProperty("vulnerabilityId")
                .addProperty("countV");


        List<KeyValueEntity> values = dataManager.loadValues(context);
        values.forEach(s -> {
            Vulnerability v = vulnerabilityRepository.findbyId(s.getValue("vulnerabilityId"));
            Long value = s.getValue("countV");
            vulnerabilityDTOs.add(new AuditVulnerabilityDTO(v.getVulnerabilityId(),v.getRiskScore(),value.intValue()));
        });
        auditVulnerabilitiesDc.setItems(vulnerabilityDTOs);
        vulnerabilityAccordion.setSummaryText("Vulnerabilities ("+vulnerabilityDTOs.size()+")");
    }

    private void showContentInformationDialog(Object content){
        DialogWindow<OverviewContentInfoDialog> window =
                dialogWindows.view(hostView, OverviewContentInfoDialog.class).build();
        window.getView().setInformationContent(content);
        window.addAfterCloseListener(event -> {
            item = window.getView().getInventoryItem();
            if(hostView instanceof AuditView auditView){
                auditView.handleInventoryItemFromOverview(item);
            }
        });
        window.open();
    }

    private JmixButton createShowButton(){
        JmixButton showButton = uiComponents.create(JmixButton.class);
        showButton.setIcon(VaadinIcon.INFO_CIRCLE.create());
        showButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        return showButton;
    }

    private void addInfoButton(){

        if(auditLicensesDataGrid.getColumnByKey("infoButton") == null) {
            auditLicensesDataGrid.addComponentColumn(auditLicenseDTO -> {
                JmixButton showButton = createShowButton();
                showButton.addClickListener(click -> openOverviewContentDialog(auditLicenseDTO));
                return showButton;
            }).setKey("infoButton");
        }
        if(auditVulnerabilityDataGrid.getColumnByKey("infoButton") == null){
            auditVulnerabilityDataGrid.addComponentColumn(auditVulnerabilityDTO -> {
                JmixButton showButton = createShowButton();
                showButton.addClickListener(click -> openOverviewContentDialog(auditVulnerabilityDTO));
                return showButton;
            }).setKey("infoButton");
        }
    }

    private void openOverviewContentDialog(Object content){
        if(content instanceof AuditLicenseDTO auditLicenseDTO){
            List<License> licenses = licenseRepository.findLicensesByLicenseName(auditLicenseDTO.getLicenseName());
            showContentInformationDialog(licenses.getFirst());
        }
        if(content instanceof AuditVulnerabilityDTO auditVulnerabilityDTO){
            List<Vulnerability> vulnerabilities = vulnerabilityRepository
                    .findByVulnerabilityId(auditVulnerabilityDTO.getVulnerabilityName());
            showContentInformationDialog(vulnerabilities.getFirst());
        }
    }

    public InventoryItem getInventoryItem(){return item;}

}
