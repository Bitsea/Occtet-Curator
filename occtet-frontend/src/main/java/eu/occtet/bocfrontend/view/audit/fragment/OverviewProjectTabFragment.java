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
import eu.occtet.bocfrontend.model.AuditCopyrightDTO;
import eu.occtet.bocfrontend.model.AuditLicenseDTO;
import eu.occtet.bocfrontend.model.AuditVulnerabilityDTO;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.view.audit.AuditView;
import eu.occtet.bocfrontend.view.dialog.OverviewContentInfoDialog;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
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

    @Autowired
    private Messages messages;

    private InventoryItem item;


    public void setDefaultAccordionValues(){
        copyrightAccordion.setSummaryText("Copyrights (0)");
        licensesAccordion.setSummaryText("Licenses (0)");
        vulnerabilityAccordion.setSummaryText("Vulnerabilities (0)");
    }

    public void setProjectOverview(@Nonnull Project project){
        this.project = dataContext.merge(project);
        setAllProjectInformation(this.project);
        addInfoButton(this.project);
    }

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
    }

    private void setAllProjectInformation(Project project ){
        setCopyrights(project);
        setLicenses(project);
        setVulnerabilities(project);
    }

    private void setCopyrights(Project project){

        List<AuditCopyrightDTO> auditCopyrightDTOs = new ArrayList<>();

        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query("""
                            select cr.id as copyrightId, count(distinct cl) as countCL
                            from InventoryItem i
                            join i.softwareComponent s
                            join s.copyrights cr
                            join cr.codeLocations cl
                            join i.project p
                            where p.id = :project_id
                            group by cr.id
                      \s""")
                        .setParameter("project_id",project.getId()))
                .addProperty("copyrightId")
                .addProperty("countCL");


        List<KeyValueEntity> values = dataManager.loadValues(context);
        values.forEach(s -> {
            Copyright copyright = copyrightRepository.findCopyrightById(s.getValue("copyrightId"));
            Long value = s.getValue("countCL");
            auditCopyrightDTOs.add(new AuditCopyrightDTO(copyright.getCopyrightText(),value.intValue()));
        });

        auditCopyrightDc.setItems(auditCopyrightDTOs);
        copyrightAccordion.setSummaryText(messages.getMessage("eu.occtet.bocfrontend.view/overvoewProjectTabFragment.copyrightAccordion.summary") + " ("+auditCopyrightDTOs.size()+")");

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
        licensesAccordion.setSummaryText(messages.getMessage("eu.occtet.bocfrontend.view/overvoewProjectTabFragment.licenseAccordion.summary") + " ("+licensesDTOs.size()+")");
    }

    private void setVulnerabilities(Project project){

        List<AuditVulnerabilityDTO> vulnerabilityDTOs = new ArrayList<>();

        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query("""
                            select v.id as vulnerabilityId, count(s) as countV
                            from InventoryItem i
                            join i.softwareComponent s
                            join s.vulnerabilities v
                            join i.project p
                            where p.id = :project_id
                            group by v.id
                       """)
                        .setParameter("project_id",project.getId()))
                .addProperty("vulnerabilityId")
                .addProperty("countV");


        List<KeyValueEntity> values = dataManager.loadValues(context);
        values.forEach(s -> {
            Vulnerability v = vulnerabilityRepository.findVulnerabilityById(s.getValue("vulnerabilityId"));
            Long value = s.getValue("countV");
            vulnerabilityDTOs.add(new AuditVulnerabilityDTO(v.getVulnerabilityId(),v.getRiskScore(),value.intValue()));
        });
        auditVulnerabilitiesDc.setItems(vulnerabilityDTOs);
        vulnerabilityAccordion.setSummaryText(messages.getMessage("eu.occtet.bocfrontend.view/overvoewProjectTabFragment.vulnerabilityAccordion.summary") + " ("+vulnerabilityDTOs.size()+")");
    }

    private void showContentInformationDialog(Object content){
        DialogWindow<OverviewContentInfoDialog> window =
                dialogWindows.view(hostView, OverviewContentInfoDialog.class).build();
        window.getView().setInformationContent(content,project);
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

    private void addInfoButton(Project project){

        if(auditLicensesDataGrid.getColumnByKey("infoButton") == null) {
            auditLicensesDataGrid.addComponentColumn(auditLicenseDTO -> {
                JmixButton showButton = createShowButton();
                showButton.addClickListener(click -> openOverviewContentDialog(auditLicenseDTO,project));
                return showButton;
            }).setKey("infoButton");
        }
        if(auditVulnerabilityDataGrid.getColumnByKey("infoButton") == null){
            auditVulnerabilityDataGrid.addComponentColumn(auditVulnerabilityDTO -> {
                JmixButton showButton = createShowButton();
                showButton.addClickListener(click -> openOverviewContentDialog(auditVulnerabilityDTO,project));
                return showButton;
            }).setKey("infoButton");
        }
    }

    private void openOverviewContentDialog(Object content, Project project){
        if(content instanceof AuditLicenseDTO auditLicenseDTO){
            List<License> licenses = licenseRepository.findLicensesByLicenseNameAndProject(auditLicenseDTO.getLicenseName(),project);
            showContentInformationDialog(licenses.getFirst());
        }
        if(content instanceof AuditVulnerabilityDTO auditVulnerabilityDTO){
            List<Vulnerability> vulnerabilities = vulnerabilityRepository
                    .findByVulnerabilityIdAndProject(auditVulnerabilityDTO.getVulnerabilityName(),project);
            showContentInformationDialog(vulnerabilities.getFirst());
        }
    }

    public InventoryItem getInventoryItem(){return item;}

}
