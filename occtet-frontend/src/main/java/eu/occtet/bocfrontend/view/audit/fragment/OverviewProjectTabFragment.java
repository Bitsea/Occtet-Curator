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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.entity.*;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.ViewComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FragmentDescriptor("OverviewProjectTabFragment.xml")
public class OverviewProjectTabFragment extends Fragment<VerticalLayout>{

    private static final Logger log = LogManager.getLogger(OverviewProjectTabFragment.class);

    private Project project;

    @ViewComponent
    private AccordionPanel copyrightAccordion;

    @ViewComponent
    private AccordionPanel licensesAccordion;

    @ViewComponent
    private AccordionPanel vulnerabilityAccordion;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<Copyright> copyrightsDc;

    @ViewComponent
    private CollectionContainer<License> licensesDc;

    @ViewComponent
    private CollectionContainer<Vulnerability> vulnerabilitiesDc;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;


    public void setBasicAccordionValues(){
        copyrightAccordion.setSummaryText("Copyright (0)");
        licensesAccordion.setSummaryText("Licenses (0)");
        vulnerabilityAccordion.setSummaryText("Vulnerabilities (0)");
    }

    public void setProject(@Nonnull Project project){
        this.project = dataContext.merge(project);
        List<InventoryItem> items = inventoryItemRepository.findByProject(project);
        List<SoftwareComponent> components = new ArrayList<>();
        if(items != null){
            for(InventoryItem item : items){
                components.add(item.getSoftwareComponent());
            }

        }
        setCopyrights(components);
        setLicenses(components);
        setVulnerabailities(components);
    }

    private void setCopyrights(List<SoftwareComponent> components){
        Set<Copyright> setCopyrights = new HashSet<>();
        for(SoftwareComponent component : components){
            if(component != null){
                List<Copyright> copyrights = component.getCopyrights();
                if(copyrights != null){
                    setCopyrights.addAll(copyrights);
                }
            }
        }
        copyrightsDc.setItems(setCopyrights);
        copyrightAccordion.setSummaryText("Copyrights ("+setCopyrights.size()+")");
    }

    private void setLicenses(List<SoftwareComponent> softwareComponents){
        Set<License> setLicenses = new HashSet<>();
        for(SoftwareComponent s : softwareComponents){
            if(s != null){
                List<License> licenses = s.getLicenses();
                if(licenses != null){
                    setLicenses.addAll(licenses);
                }
            }
        }
        licensesDc.setItems(setLicenses);
        licensesAccordion.setSummaryText("Licenses ("+setLicenses.size()+")");
    }

    private void setVulnerabailities(List<SoftwareComponent> softwareComponents){
        Set<Vulnerability> setVulnerabilities = new HashSet<>();
        for(SoftwareComponent s : softwareComponents){
            if(s != null){
                List<Vulnerability> vulnerabilities = s.getVulnerabilities();
                if(vulnerabilities != null){
                    setVulnerabilities.addAll(vulnerabilities);
                }
            }
        }
        vulnerabilitiesDc.setItems(setVulnerabilities);
        vulnerabilityAccordion.setSummaryText("Vulnerabilities ("+setVulnerabilities.size()+")");

    }
}
