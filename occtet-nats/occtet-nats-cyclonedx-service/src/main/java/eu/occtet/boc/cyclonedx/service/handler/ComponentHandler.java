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
        Metadata metadata = bom.getMetadata();
        Component comp = metadata != null ? metadata.getComponent() : null;
        InventoryItem mainParent = null;

        try {
            log.debug("handling metadata component");
            if (metadata != null && comp != null) {
                // set project name (else there can be strange names)
                comp.setName(context.getProject().getProjectName());

                mainParent = processAllComponents(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, comp, context);
                context.getMainInventoryItems().add(mainParent);
                mainParent.getSoftwareComponent().setDetailsUrl(context.getProject().getRepositoryURL());
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

                processComponentRecursive(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, component, context, 0, withTestLibraries, mainParent);

            } catch (Exception e) {
                log.error("Skipping component {} due to error: {}", component.getName(), e.getMessage(), e);
            }
            count++;
            int percent = (int) ((40.0 * count) / bom.getComponents().size());
            if (percent % 5 == 0 && progressCallback != null) progressCallback.accept(percent);
        }

        handleDependencies(bom, inventoryItemsToSave);

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
        if (!context.getLicenseCache().isEmpty())
            licenseRepository.saveAll(context.getLicenseCache().values());
        if (!context.getUsageLicenseCache().isEmpty())
            softwareComponentLicenseUsageRepository.saveAll(context.getUsageLicenseCache().values());
    }

    public void processComponentRecursive(Set<Copyright> copyrightsToSave, Set<InventoryItem> inventoryItemsToSave,
                                          Set<SoftwareComponent> softwareComponentsToSave, Component component,
                                          CycloneDxImportContext context, int depth, Boolean withTestLibraries, InventoryItem parentItem) {

        InventoryItem currentItem = processAllComponents(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, component, context);

        if (currentItem != null) {
            // fallback to first main inventory item if no parent is provided
            InventoryItem targetParent = parentItem;
            if (targetParent == null && !context.getMainInventoryItems().isEmpty()) {
                targetParent = context.getMainInventoryItems().iterator().next();
            }

            // bidirectional parent relation
            if (targetParent != null && !targetParent.equals(currentItem)) {
                log.debug("Setting parent {} for dependency {}", targetParent.getInventoryName(), currentItem.getInventoryName());

                currentItem.setParent(targetParent);

                // important
                if (!targetParent.getDependencies().contains(currentItem)) {
                    targetParent.getDependencies().add(currentItem);
                }
            }
        }

        if (component.getComponents() != null && !component.getComponents().isEmpty()) {
            for (Component childComponent : component.getComponents()) {
                if (!withTestLibraries && isExcluded(childComponent)) {
                    continue;
                }

                processComponentRecursive(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave,
                        childComponent, context, depth + 1, withTestLibraries, currentItem);
            }
        }
    }

    private boolean isExcluded(Component component) {
        return component.getScope() != null && "excluded".equals(component.getScope().getScopeName());
    }

    private void handleDependencies(Bom bom, Set<InventoryItem> inventoryItemsToSave) {
        if (bom.getDependencies() != null && !bom.getDependencies().isEmpty()) {
            log.debug("Resolving dependency graph for {} entries", bom.getDependencies().size());

            Map<String, InventoryItem> bomRefToItemMap = new HashMap<>();

            for (InventoryItem item : inventoryItemsToSave) {
                if (item.getSoftwareComponent() != null && item.getSoftwareComponent().getBomRef() != null) {
                    bomRefToItemMap.put(item.getSoftwareComponent().getBomRef(), item);
                }
            }

            for (org.cyclonedx.model.Dependency dependency : bom.getDependencies()) {
                String parentRef = dependency.getRef();
                InventoryItem parentItem = bomRefToItemMap.get(parentRef);

                if (parentItem != null && dependency.getDependencies() != null) {
                    for (org.cyclonedx.model.Dependency childDependency : dependency.getDependencies()) {
                        String childRef = childDependency.getRef();
                        InventoryItem childItem = bomRefToItemMap.get(childRef);

                        if (childItem != null) {
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
            InventoryItem item = parseSinglePackage(component, context, copyrightsToSave);
            if (item != null) {
                context.getInventoryItems().add(item);
                inventoryItemsToSave.add(item);
                if (item.getSoftwareComponent() != null) {
                    softwareComponentsToSave.add(item.getSoftwareComponent());
                }
                context.getItemComponentRefCache().put(component.getBomRef(), item);
            }
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

        // delete suffixes like "-source-artifact" or "-vcs" from package name to avoid duplicates in inventory
        if (packageName != null) {
            if (packageName.endsWith("-source-artifact")) {
                packageName = packageName.substring(0, packageName.length() - "-source-artifact".length());
            } else if (packageName.endsWith("-vcs")) {
                packageName = packageName.substring(0, packageName.length() - "-vcs".length());
            }
        }

        String version = component.getVersion() != null ? component.getVersion() : "unknown";

        SoftwareComponent sc = context.getComponentCache().get(component.getBomRef());

        if (sc == null) {
            sc = softwareComponentService.getOrCreateSoftwareComponent(packageName, version, context.getProject().getOrganization(), component.getType() != null ? component.getType().getTypeName() : null);
            context.getComponentCache().put(component.getBomRef(), sc);
        }

        sc.setBomRef(component.getBomRef());
        sc.setPurl(component.getPurl());
        context.getComponentVulnerabilityCache().put(component.getBomRef(), sc);

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
            log.debug("adding inventoryItem {} to cache", inventoryName);
            context.getInventoryCache().put(inventoryName, inventoryItem);
        } else {
            log.debug("adding inventoryItem {} to cache", inventoryName);
            inventoryItem = context.getInventoryCache().get(inventoryName);
        }

        inventoryItem.setSoftwareComponent(sc);
        inventoryItemService.sortViolationsAndIssues(ortIssues, ortViolations, inventoryItem);

        if ((component.getEvidence() != null && component.getEvidence().getOccurrences() != null)
                || (component.getType() != null && "file".equals(component.getType().getTypeName()))) {
            handleFiles(component, inventoryItem);
        }

        context.getInventoryItems().add(inventoryItem);
        File file = !inventoryItem.getProject().getFiles().isEmpty() ? inventoryItem.getProject().getFiles().iterator().next() : null;

        if (component.getCopyright() != null || (component.getEvidence() != null && component.getEvidence().getCopyright() != null)) {
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
        log.info("created softwareComponent: {}", packageName);

        return inventoryItem;
    }

    private List<Copyright> handleCopyrights(Component component, CycloneDxImportContext context, Set<Copyright> copyrightsToSave, File file) {
        Set<String> allCopyrightsTexts = new HashSet<>();
        if (component.getCopyright() != null) {
            allCopyrightsTexts.add(component.getCopyright());
        }
        if (component.getEvidence() != null && component.getEvidence().getCopyright() != null) {
            for (org.cyclonedx.model.Copyright copyright : component.getEvidence().getCopyright()) {
                allCopyrightsTexts.add(copyright.getText());
            }
        }
        Map<String, Copyright> copyrightMap = copyrightService.findOrCreateBatch(allCopyrightsTexts,
                context.getProject().getOrganization(), file);

        copyrightsToSave.addAll(copyrightMap.values());
        return new ArrayList<>(copyrightMap.values());
    }

    private List<File> handleFiles(Component component, InventoryItem inventoryItem) {
        Set<String> allFilePaths = new HashSet<>();
        if ("file".equals(component.getType().getTypeName())) {
            allFilePaths.add(component.getName());
        }
        if (component.getEvidence() != null && component.getEvidence().getOccurrences() != null) {
            for (Occurrence occ : component.getEvidence().getOccurrences()) {
                allFilePaths.add(occ.getLocation());
            }
        }

        Map<String, File> locationMap = fileService.findOrCreateBatch(allFilePaths, inventoryItem);

        Project project = inventoryItem.getProject();
        project.addFiles(new HashSet<>(locationMap.values()));
        return new ArrayList<>(locationMap.values());
    }
}