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
import eu.occtet.bocfrontend.view.dialog.OverviewContentInfoDialog;
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
        setCopyrights(components,codeLocations);
        setLicenses(components);
        setVulnerabilities(components);

        auditLicensesDataGrid.addComponentColumn(auditLicenseDTO -> {
            JmixButton showButton = createShowButton();
            showButton.addClickListener(click -> {
                List<License> licenses = licenseRepository.findLicensesByLicenseName(auditLicenseDTO.getLicenseName());
                showContentInformationDialog(licenses.getFirst());
            });
        return showButton;
        });
        auditVulnerabilityDataGrid.addComponentColumn(auditVulnerabilityDTO -> {
            JmixButton showButton = createShowButton();
            showButton.addClickListener(click->{
                List<Vulnerability> vulnerabilities = vulnerabilityRepository
                        .findByVulnerabilityId(auditVulnerabilityDTO.getVulnerabilityName());
                showContentInformationDialog(vulnerabilities.getFirst());
            });
            return showButton;
        });
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

    private void setLicenses(List<SoftwareComponent> softwareComponents){

        List<AuditLicenseDTO> licensesDTOs = new ArrayList<>();
        Set<License> allLicenses = new HashSet<>();

        allLicenses.addAll(softwareComponents.stream().map(SoftwareComponent::getLicenses)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toSet()));

        allLicenses.forEach(license -> {
            int count = 0;
            for(SoftwareComponent sc : softwareComponents){
                List<License> licenses = sc.getLicenses();
                if(licenses != null){count += Collections.frequency(licenses,license);}
            }
            licensesDTOs.add(new AuditLicenseDTO(license.getLicenseName(),count));
        });
        auditLicensesDc.setItems(licensesDTOs);
        licensesAccordion.setSummaryText("Licenses ("+licensesDTOs.size()+")");
    }

    private void setVulnerabilities(List<SoftwareComponent> softwareComponents){

        List<AuditVulnerabilityDTO> vulnerabilityDTOs = new ArrayList<>();
        Set<Vulnerability> allVulnerabilities = new HashSet<>();

        allVulnerabilities.addAll(softwareComponents.stream().map(SoftwareComponent::getVulnerabilities)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toSet()));

        allVulnerabilities.forEach(vulnerability -> {
            int count = 0;
            for(SoftwareComponent sc : softwareComponents){
                List<Vulnerability> vulnerabilities = sc.getVulnerabilities();
                if(vulnerabilities != null){count += Collections.frequency(vulnerabilities,vulnerability);}
            }
            vulnerabilityDTOs.add(new AuditVulnerabilityDTO(vulnerability.getVulnerabilityId(),vulnerability.getRiskScore(),count));
        });
        auditVulnerabilitiesDc.setItems(vulnerabilityDTOs);
        vulnerabilityAccordion.setSummaryText("Vulnerabilities ("+vulnerabilityDTOs.size()+")");
    }

    private void showContentInformationDialog(Object content){
        DialogWindow<OverviewContentInfoDialog> window =
                dialogWindows.view(hostView, OverviewContentInfoDialog.class).build();
        window.getView().setInformationContent(content);
        window.open();
    }

    private JmixButton createShowButton(){
        JmixButton showButton = uiComponents.create(JmixButton.class);
        showButton.setIcon(VaadinIcon.INFO_CIRCLE.create());
        showButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        return showButton;
    }
}
