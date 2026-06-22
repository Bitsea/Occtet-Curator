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

package eu.occtet.boc.cyclonedx.service.handler;

import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.cyclonedx.context.CycloneDxImportContext;
import eu.occtet.boc.cyclonedx.service.CopyrightService;
import eu.occtet.boc.cyclonedx.service.FileService;
import eu.occtet.boc.cyclonedx.service.InventoryItemService;
import eu.occtet.boc.cyclonedx.service.SoftwareComponentService;
import eu.occtet.boc.entity.Copyright;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.*;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
public class ComponentHandler {

    private static final Logger log = LogManager.getLogger(ComponentHandler.class);

    @Autowired
    private LicenseHandler licenseHandler;
    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private FileService fileService;
    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private OrtIssueRepository ortIssueRepository;
    @Autowired
    private OrtViolationRepository ortViolationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private SoftwareComponentLicenseUsageRepository softwareComponentLicenseUsageRepository;

    public void processAllPackages(CycloneDxImportContext context, Consumer<Integer> progressCallback, Bom bom, Boolean withTestLibraries) {

            int count = 0;
            Set<InventoryItem> inventoryItemsToSave = new HashSet<>();
            Set<SoftwareComponent> softwareComponentsToSave = new HashSet<>();
            Set<Copyright> copyrightsToSave = new HashSet<>();
            Metadata metadata=  bom.getMetadata();
            Component comp = metadata.getComponent();
            InventoryItem mainParent= null;
        try {
            log.debug("handling metadata component");
            if (metadata != null && comp != null) {
                mainParent = processAllComponents(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, comp, context);
                context.getMainInventoryItems().add(mainParent);
            }
        } catch (Exception e) {
            log.error("Error processing metadata component: {}", e.getMessage(), e);
        }

        if (bom.getComponents() == null) return;


        for (Component component : bom.getComponents()) {
            log.debug("going through all components of sbom");

            try {
                boolean isExcluded = isExcluded(component);
                if (!withTestLibraries && isExcluded) {
                    continue;
                }

                // if something is going wrong it will be catched and logged, next component will be handled
                processComponentRecursive(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, component, context, 0, withTestLibraries, mainParent);

            } catch (Exception e) {
                log.error("Skipping component {} due to error: {}", component.getName(), e.getMessage(), e);
            }
                count++;
                int percent = (int) ((40.0 * count) / bom.getComponents().size());
                if (percent % 5 == 0 && progressCallback!= null) progressCallback.accept(percent);
            }

        handleDependencies(bom, inventoryItemsToSave, mainParent);

        log.debug("saving all entities creating of sbom");

            projectRepository.save(context.getProject());
            if (!copyrightsToSave.isEmpty()) {
                copyrightRepository.saveAll(copyrightsToSave);
            }
            if (!inventoryItemsToSave.isEmpty()) {
                inventoryItemRepository.saveAll(inventoryItemsToSave);
            }
            if (!softwareComponentsToSave.isEmpty()) {
                softwareComponentRepository.saveAll(softwareComponentsToSave);
            }
            if(!context.getLicenseCache().isEmpty())
                licenseRepository.saveAll(context.getLicenseCache().values());
            if(!context.getUsageLicenseCache().isEmpty())
                softwareComponentLicenseUsageRepository.saveAll(context.getUsageLicenseCache().values());

    }

    /**
     * recursive method to process components and their subcomponents, if there are any
     * @param copyrightsToSave
     * @param inventoryItemsToSave
     * @param softwareComponentsToSave
     * @param component
     * @param context
     * @param depth
     * @param withTestLibraries
     */
    public void processComponentRecursive(Set<Copyright> copyrightsToSave, Set<InventoryItem> inventoryItemsToSave,
                                           Set<SoftwareComponent> softwareComponentsToSave, Component component,
                                           CycloneDxImportContext context, int depth, Boolean withTestLibraries, InventoryItem parentItem) {


        // process component of current level
        InventoryItem currentItem= processAllComponents(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, component, context);

        if (depth == 0) {
            context.getMainInventoryItems().add(currentItem);
        }

        //handle relation between children and parent
        if (currentItem != null && parentItem != null) {
            log.debug("setting parent {}", parentItem.getInventoryName());
            currentItem.setParent(parentItem);
        }

        // are there subcomponents?
        if (component.getComponents() != null && !component.getComponents().isEmpty()) {
            for (Component childComponent : component.getComponents()) {

                //filtering if tests not needed
                if (!withTestLibraries && isExcluded(childComponent)) {
                    continue;
                }

                //recursion to go a level deeper
                processComponentRecursive(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave,
                        childComponent, context, depth + 1, withTestLibraries, currentItem);
            }
        }
    }

    private boolean isExcluded(Component component) {
        return component.getScope() != null && "excluded".equals(component.getScope().getScopeName());
    }

    private void handleDependencies(Bom bom, Set<InventoryItem> inventoryItemsToSave, InventoryItem mainParent){
        //solve dependency tree
        if (bom.getDependencies() != null && !bom.getDependencies().isEmpty()) {
            log.debug("Resolving dependency graph for {} entries", bom.getDependencies().size());

            // look-up map bom-ref -> item
            Map<String, InventoryItem> bomRefToItemMap = new HashMap<>();

            for (InventoryItem item : inventoryItemsToSave) {
                if (item.getSoftwareComponent().getBomRef() != null) {
                    bomRefToItemMap.put(item.getSoftwareComponent().getBomRef(), item);
                }
            }

            // go through graph
            for (org.cyclonedx.model.Dependency dependency : bom.getDependencies()) {
                String parentRef = dependency.getRef();
                InventoryItem parentItem = bomRefToItemMap.get(parentRef);

                // if we know parent and it has declared dependency
                if (parentItem != null && dependency.getDependencies() != null) {
                    for (org.cyclonedx.model.Dependency childDependency : dependency.getDependencies()) {
                        String childRef = childDependency.getRef();
                        InventoryItem childItem = bomRefToItemMap.get(childRef);

                        if (childItem != null) {
                            // magic: fit parent to children
                            parentItem.getDependencies().add(childItem);
                            log.debug("Linked dependency: {} -> {}", parentItem.getInventoryName(), childItem.getInventoryName());
                        }
                    }
                }
            }
        }
    }

    private InventoryItem processAllComponents(Set<Copyright> copyrightsToSave, Set<InventoryItem> inventoryItemsToSave, Set<SoftwareComponent> softwareComponentsToSave, Component component, CycloneDxImportContext context) {
        try {

            //components in cyclonedx can also be files, so here a inventoryItem is automatically also created for a file
            InventoryItem item = parseSinglePackage(component, context, copyrightsToSave);
            context.getInventoryItems().add(item);
            inventoryItemsToSave.add(item);
            if (item.getSoftwareComponent() != null) {
                softwareComponentsToSave.add(item.getSoftwareComponent());
            }
            //to reference inventoryItem to the componentRef
            context.getItemComponentRefCache().put(component.getBomRef(), item);


            return item;

        } catch (Exception e) {
            log.error("Error retrieving cycloneDx object for component: {}", component.getPurl(), e);
            return null;
        }

    }

    private InventoryItem parseSinglePackage(Component component, CycloneDxImportContext context,
                                            Set<Copyright> copyrightsToSave) {

        List<OrtIssue> ortIssues = ortIssueRepository.findByProject(context.getProject());
        List<OrtViolation> ortViolations = ortViolationRepository.findByProject(context.getProject());

        log.info("Looking at package: {}", component.getPurl());

        String packageName = component.getName();
        String version = component.getVersion() != null ? component.getVersion() : "unknown";

        SoftwareComponent sc = context.getComponentCache().get(component.getBomRef());

        if (sc == null) {
            sc = softwareComponentService.getOrCreateSoftwareComponent(packageName, version, context.getProject().getOrganization(), component.getType().getTypeName());
            context.getComponentCache().put(component.getBomRef(), sc);

        }
        //setting this always new for each sbom the value is different, needed later for sorting vulnerabilities etc
        sc.setBomRef(component.getBomRef());
        sc.setPurl(component.getPurl());
        //to handle vulnerabilities later + faster
        context.getComponentVulnerabilityCache().put(component.getBomRef(), sc);
        //get License from package
        LicenseChoice licenseChoices = component.getLicenses();

        String packageLicenseString = licenseHandler.createUsageLicenses(licenseChoices, context,
                sc, context.getProject().getOrganization());


        String inventoryName = sc.getName();
        if (!inventoryName.contains(version)) inventoryName += " " + version;
        inventoryName += " (" + packageLicenseString + ")";

        InventoryItem inventoryItem;
        if (!context.getInventoryCache().containsKey(inventoryName)) {
            log.debug("creating new inventoryItem");
            inventoryItem = inventoryItemService.getOrCreateInventoryItem(inventoryName, sc,
                    context.getProject(),
                    context.getProject().getOrganization());
            inventoryItem.setCurated(false);

        } else {
            inventoryItem = context.getInventoryCache().get(inventoryName);
        }

        inventoryItem.setSoftwareComponent(sc);
        List<File> files = new ArrayList<>();
        inventoryItemService.sortViolationsAndIssues(ortIssues, ortViolations, inventoryItem);

        if ((component.getEvidence() != null && component.getEvidence().getOccurrences() != null)
                || (component.getType() != null && "file".equals(component.getType().getTypeName()))) {
            files = handleFiles(component, inventoryItem);
        }

        //to keep track of current inventoryItems
        context.getInventoryItems().add(inventoryItem);
        File file= files.isEmpty() ? null : files.getFirst();

        if (component.getCopyright() != null || (component.getEvidence()!= null && component.getEvidence().getCopyright() != null)) {
            List<Copyright> copyrightList = handleCopyrights(component, context, copyrightsToSave, file);
            sc.setCopyrights(copyrightList);
        }

        String downloadLocation = "";

        if (component.getExternalReferences() != null) {
            for (ExternalReference ref : component.getExternalReferences()) {

                downloadLocation = ref.getUrl();

            }
        }

        sc.setDetailsUrl(downloadLocation);

        log.info("created inventoryItem: {}", inventoryName);
        log.info("created softwareComponent: {}", component.getName());

        return inventoryItem;
    }

    private List<Copyright> handleCopyrights(Component component, CycloneDxImportContext context, Set<Copyright> copyrightsToSave,File file){
        Set<String> allCopyrightsTexts = new HashSet<>();
        allCopyrightsTexts.add(component.getCopyright());

        for (org.cyclonedx.model.Copyright copyright : component.getEvidence().getCopyright()) {

                String copyrightText = copyright.getText();
                allCopyrightsTexts.add(copyrightText);
        }
        Map<String, Copyright> copyrightMap = copyrightService.findOrCreateBatch(allCopyrightsTexts,
                context.getProject().getOrganization(), file);


        copyrightsToSave.addAll(copyrightMap.values());
        return new ArrayList<>(copyrightMap.values());
    }

    private List<File> handleFiles(Component component, InventoryItem inventoryItem){

        Set<String> allFilePaths= new HashSet<>();
        if(("file").equals(component.getType().getTypeName())) {
            allFilePaths.add(component.getName());
        }
        if(component.getEvidence() != null && component.getEvidence().getOccurrences() != null) {
            for (Occurrence occ : component.getEvidence().getOccurrences()) {
                String filepath = occ.getLocation();
                allFilePaths.add(filepath);
            }
        }

        Map<String, File> locationMap = fileService.findOrCreateBatch(allFilePaths, inventoryItem);

        Project project= inventoryItem.getProject();
        project.addFiles(new HashSet<>(locationMap.values()));
        return new ArrayList<>(locationMap.values());

    }


}