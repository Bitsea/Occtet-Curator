/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.export.service.handler;

import org.cyclonedx.parsers.JsonParser;
import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.SoftwareComponentLicenseUsageRepository;
import eu.occtet.boc.dao.VexDataRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.service.ProgressReportingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.*;
import org.cyclonedx.model.License;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.cyclonedx.model.metadata.ToolInformation;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ComponentHandler  extends ProgressReportingService {

    private static final Logger log = LogManager.getLogger(ComponentHandler.class);

    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private SoftwareComponentLicenseUsageRepository softwareComponentLicenseUsageRepository;
    @Autowired
    private VexDataRepository vexDataRepository;
    @Autowired
    private VexDataHandler vexDataHandler;

    public Bom handleComponents(Project project, Consumer<Integer> progressCallback, Bom bom, Boolean enriched){
        //get already created metadata from bom
        Metadata metadata = bom.getMetadata();

        List<InventoryItem> inventoryItemList= inventoryItemRepository.findAllByProject(project);
        List<File> files= fileRepository.findAllByProject(project);
        List<SoftwareComponentLicenseUsage> usages= softwareComponentLicenseUsageRepository.findUsageByProject(project);
        if(inventoryItemList.isEmpty()){
            log.error("No InventoryItems available");
            return null;
        }

        // mapping for all items, together with parent ID for faster processing and sorting
        Map<Long, List<InventoryItem>> childrenMap = inventoryItemList.stream()
                .filter(item -> item.getParent() != null)
                .collect(Collectors.groupingBy(item -> item.getParent().getId()));

        //mapping for selecting right vulnerabilities
        Map<Long, List<InventoryItem>> componentToItemsMap = inventoryItemList.stream()
                .filter(item -> item.getSoftwareComponent() != null)
                .collect(Collectors.groupingBy(item -> item.getSoftwareComponent().getId()));

        // find all top level items (Items, which are not a child -> parent == null)
        List<InventoryItem> rootItems = inventoryItemList.stream()
                .filter(item -> item.getParent() == null)
                .toList();

        //get all vexData into List
        List<Long> componentIds = inventoryItemList.stream()
                .map(InventoryItem::getSoftwareComponent)
                .filter(Objects::nonNull)
                .map(SoftwareComponent::getId)
                .distinct()
                .collect(Collectors.toList());


        List<VexData> allProjectVexData = vexDataRepository.findBySoftwareComponentIds(componentIds);


        //map usageLicenses to softwarecompoennt for faster sorting
        Map<Long, List<SoftwareComponentLicenseUsage>> licenseMap = usages.stream()
                .filter(usage -> usage.getSoftwareComponent() != null)
                .collect(Collectors.groupingBy(usage -> usage.getSoftwareComponent().getId()));



        //sort files to inventoryItem
        Map<Long, List<File>> filesMap = new HashMap<>();
        for (File file : files) {
            if (file.getInventoryItems() != null) {
                for (InventoryItem item : file.getInventoryItems()) {
                    if (item != null && item.getId() != null) {
                        // if id not yet in map, create list and add file
                        filesMap.computeIfAbsent(item.getId(), k -> new ArrayList<>()).add(file);
                    }
                }
            }
        }

        addDependencySection(inventoryItemList, bom, rootItems);

        log.debug("fetched all important entities from DB and mapped them");

        if(progressCallback!= null)
            progressCallback.accept(5);

        Component mainComponent;

        if (rootItems.size() == 1) {
            log.debug("one main component");
            // best case: there is a single main item
            log.debug("go through children");
            mainComponent = mapInventoryItemToComponent(rootItems.getFirst(),progressCallback, childrenMap, filesMap, licenseMap, enriched);
            mainComponent.setType(Component.Type.APPLICATION);

            if (mainComponent.getComponents() != null) {
                bom.setComponents(mainComponent.getComponents());
                mainComponent.setComponents(null);
            }
        } else {
            // Fallback: several top-level items found
            log.debug("create root component here");
            mainComponent = new Component();
            mainComponent.setBomRef("virtual-project-root-" + project.getProjectName().replaceAll("\\s+", "-").toLowerCase());
            mainComponent.setName(project.getProjectName());
            mainComponent.setVersion(project.getVersion());
            mainComponent.setType(Component.Type.APPLICATION);
            mainComponent.setDescription("SBOM-root for audit-project: " + project.getProjectName());

            log.debug("go through children");
            List<Component> childComponents = rootItems.stream()
                    .map(rootItem -> mapInventoryItemToComponent(rootItem,progressCallback, childrenMap, filesMap, licenseMap, enriched))
                    .collect(Collectors.toList());

            bom.setComponents(childComponents);
        }
        metadata.setComponent(mainComponent);

        addVulnerabilities(bom,inventoryItemList, allProjectVexData, componentToItemsMap);

        return bom;

    }




    /**
     * recursive mapping function for items and their softwareComponent to the SBOM component
     * @param item
     * @return
     */
    private Component mapInventoryItemToComponent(InventoryItem item, Consumer<Integer> progressCallback,
                                                  Map<Long, List<InventoryItem>> childrenMap, Map<Long, List<File>> filesMap, Map<Long, List<SoftwareComponentLicenseUsage>> licenseMap, boolean enrichLicenseText) {
        SoftwareComponent softComp = item.getSoftwareComponent();
        if (softComp == null) {
            return null;
        }

        Component cdComponent = new Component();
        cdComponent.setName(softComp.getName());
        cdComponent.setVersion(softComp.getVersion());
        if (Component.Type.FILE.getTypeName().equals(softComp.getOriginType())) {
            cdComponent.setType(Component.Type.FILE);
        } else {
            cdComponent.setType(Component.Type.LIBRARY);// standard
        }
        cdComponent.setBomRef(item.getInventoryName());
        //add associated files
        List<File> associatedFiles = filesMap.get(item.getId());
        addFiles(cdComponent, associatedFiles);
        addCopyrights(cdComponent, associatedFiles);

        addLicenses(cdComponent, softComp, licenseMap, enrichLicenseText);
        addProperties(cdComponent, item);

        if(progressCallback!= null)
            progressCallback.accept(1);

        // recursion: we get the children very fast from this map
        List<InventoryItem> internalChildren = childrenMap.get(item.getId());
        if (internalChildren != null && !internalChildren.isEmpty()) {
            List<Component> childComponents = new ArrayList<>();
            for (InventoryItem childItem : internalChildren) {
                Component childComponent = mapInventoryItemToComponent(childItem,progressCallback, childrenMap, filesMap, licenseMap, enrichLicenseText);
                if (childComponent != null) {
                    childComponents.add(childComponent);
                }
            }
            cdComponent.setComponents(childComponents);
        }

        return cdComponent;
    }

    private void addProperties(Component cdComponent, InventoryItem item){
        log.debug("add properties");
        // detailed data via properties also added for main item
        if (item.getPriority() != null) {
            Property priorityProp = new Property();
            priorityProp.setName("boc:audit:priority");
            priorityProp.setValue(item.getPriority().toString());
            cdComponent.addProperty(priorityProp);
        }

        if (item.getExternalNotes() != null || !item.getExternalNotes().isEmpty()) {
            Property notesProp = new Property();
            notesProp.setName("boc:audit:notes");
            notesProp.setValue(item.getExternalNotes());
            cdComponent.addProperty(notesProp);
        }
    }

    private void addFiles(Component cdComponent,List<File> associatedFiles ){
        log.debug("add files");
        if (associatedFiles != null && !associatedFiles.isEmpty()) {
            Evidence evidence = new Evidence();
            List<Occurrence> occurrences = new ArrayList<>();

            for (File file : associatedFiles) {
                Occurrence occurrence = new Occurrence();
                occurrence.setBomRef("occ-" + file.getArtifactPath());
                occurrence.setLocation(file.getArtifactPath());
                occurrences.add(occurrence);
            }

            evidence.setOccurrences(occurrences);
            cdComponent.setEvidence(evidence);
        }
    }

    private void addCopyrights(Component cdComponent,List<File> associatedFiles){
        Set<String> uniqueCopyrightTexts = new LinkedHashSet<>();

        // collect associated Copyrights from files
        if (associatedFiles != null) {
            associatedFiles.stream()
                    .filter(Objects::nonNull)
                    .flatMap(file -> file.getCopyrights().stream())
                    .filter(cp -> cp != null && !Boolean.TRUE.equals(cp.getGarbage()))
                    .map(Copyright::getCopyrightText)
                    .filter(text -> text != null && !text.isBlank())
                    .forEach(uniqueCopyrightTexts::add);
        }

        // if copyright found write to sbom
        if (!uniqueCopyrightTexts.isEmpty()) {
            String combinedCopyright = String.join("\n", uniqueCopyrightTexts);
            cdComponent.setCopyright(combinedCopyright);
        }
    }

    private void addLicenses(Component cdComponent, SoftwareComponent softComp,
                             Map<Long, List<SoftwareComponentLicenseUsage>> licenseMap, boolean enrichLicenseText){
        log.debug("add licenses");
        List<SoftwareComponentLicenseUsage> usages = licenseMap.get(softComp.getId());

        if (usages != null && !usages.isEmpty()) {
            LicenseChoice licenseChoice = new LicenseChoice();

            for (SoftwareComponentLicenseUsage usage : usages) {
                if (usage.getTemplate() != null) {
                    eu.occtet.boc.entity.License template = usage.getTemplate();
                    License cdLicense = new License();

                    // important for scanner: if a spdx id exists, it must be in the id of licensechoice
                    if (template.getLicenseType() != null && !template.getLicenseType().isBlank()) {
                        cdLicense.setId(template.getLicenseType());
                        cdLicense.setName(usage.getEffectiveName());
                    } else {
                        cdLicense.setName(usage.getEffectiveName());
                    }
                    if(usage.getEffectiveText()!= null) {
                        AttachmentText attachmentText = new AttachmentText();
                        String licenseText = usage.getEffectiveText();

                        //if licenseText should be enriched with copyright
                        if (enrichLicenseText && cdComponent.getCopyright() != null && !cdComponent.getCopyright().isBlank()) {
                            licenseText = cdComponent.getCopyright() + "\n\n" + licenseText;
                        }

                        attachmentText.setText(licenseText);
                        cdLicense.setLicenseText(attachmentText);
                    }

                    if (template.getDetailsUrl() != null) {
                        cdLicense.setUrl(template.getDetailsUrl());
                    }

                    licenseChoice.addLicense(cdLicense);
                }
            }

            if (licenseChoice.getLicenses() != null && !licenseChoice.getLicenses().isEmpty()) {
                cdComponent.setLicenses(licenseChoice);
            }
        }
    }

    private void addVulnerabilities(Bom bom, List<InventoryItem> inventoryItemList, List<VexData> allProjectVexData, Map<Long, List<InventoryItem>> componentToItemsMap) {
        log.debug("add vulnerabilities");
        List<org.cyclonedx.model.vulnerability.Vulnerability> cdVulnerabilities = new ArrayList<>();

        // map vexData to SoftwareComponent
        Map<Long, VexData> componentToVexMap = new HashMap<>();
        if (allProjectVexData != null) {
            for (VexData vex : allProjectVexData) {
                if (vex.getSoftwareComponent() != null) {
                    componentToVexMap.put(vex.getSoftwareComponent().getId(), vex);
                }
            }
        }

        // sind all unique components
        Set<SoftwareComponent> uniqueComponents = inventoryItemList.stream()
                .map(InventoryItem::getSoftwareComponent)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // go over each component for all vulnerabilities
        for (SoftwareComponent sc : uniqueComponents) {
            List<ComponentVulnerabilityLink> links = sc.getVulnerabilityLinks();
            if (links == null || links.isEmpty()) continue;

            // get inventoryItem from component
            List<InventoryItem> affectedItems = componentToItemsMap.get(sc.getId());
            if (affectedItems == null || affectedItems.isEmpty()) continue;

            // get vexData, can be null
            VexData vex = componentToVexMap.get(sc.getId());

            // map each vulnerability
            for (ComponentVulnerabilityLink link : links) {
                if (link == null || link.getVulnerability() == null) continue;

                Vulnerability vuln = link.getVulnerability();

                org.cyclonedx.model.vulnerability.Vulnerability cdVuln =
                        vexDataHandler.mapToCycloneDxVulnerability(vuln, vex, affectedItems);

                cdVulnerabilities.add(cdVuln);
            }
        }

        if (!cdVulnerabilities.isEmpty()) {
            bom.setVulnerabilities(cdVulnerabilities);
        }
    }


    private void addDependencySection(List<InventoryItem> inventoryItemList, Bom bom, List<InventoryItem> rootItems){
        log.debug("Building dependency section for CycloneDX BOM");
        List<org.cyclonedx.model.Dependency> bomDependencies = new ArrayList<>();

        for (InventoryItem item : inventoryItemList) {
            // Wenn das Item funktionale Abhängigkeiten in der DB hinterlegt hat
            if (item.getDependencies() != null && !item.getDependencies().isEmpty()) {

                // Verwende den Item-Namen (oder getInventoryName()) als bom-ref, wie von dir definiert
                String parentRef = item.getInventoryName();
                org.cyclonedx.model.Dependency cycloneNode = new org.cyclonedx.model.Dependency(parentRef);

                List<org.cyclonedx.model.Dependency> dependentChildren = new ArrayList<>();
                for (InventoryItem targetDep : item.getDependencies()) {
                    dependentChildren.add(new org.cyclonedx.model.Dependency(targetDep.getInventoryName()));
                }

                cycloneNode.setDependencies(dependentChildren);
                bomDependencies.add(cycloneNode);
            }
        }


        if (!bomDependencies.isEmpty()) {
            bom.setDependencies(bomDependencies);
            log.debug("Successfully added {} dependency nodes to the BOM", bomDependencies.size());
        }

    }
}


