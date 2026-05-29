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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.LicenseChoice;
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

    public void processAllPackages(CycloneDxImportContext context, Consumer<Integer> progressCallback, Bom bom) {

        try {

            int count = 0;
            Set<InventoryItem> inventoryItemsToSave = new HashSet<>();
            Set<SoftwareComponent> softwareComponentsToSave = new HashSet<>();
            Set<Copyright> copyrightsToSave = new HashSet<>();

            for (Component component : bom.getComponents()) {
                //TODO use the caches here
                processAllComponents(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, component, context, 0);

                count++;
                int percent = (int) ((40.0 * count) / bom.getComponents().size());
                if (percent % 5 == 0) progressCallback.accept(percent);
            }

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

        } catch (Exception e) {
            log.error("Error retrieving cycloneDx object: {}", e.getMessage(), e);
        }
    }

    private void processAllComponents(Set<Copyright> copyrightsToSave, Set<InventoryItem> inventoryItemsToSave, Set<SoftwareComponent> softwareComponentsToSave, Component component, CycloneDxImportContext context, int count) {
        try {
            //components in cyclonedx can also be files, so here a inventoryItem is automatically also created for a file
            SoftwareComponent sc= null;
            InventoryItem item = parseSinglePackage(component, context, copyrightsToSave, sc);
            context.getInventoryItems().add(item);
            inventoryItemsToSave.add(item);
            softwareComponentsToSave.add(sc);
            if(count==0){
                context.getMainInventoryItems().add(item.getId());
            }
            if(component.getComponents()!= null){
                for(Component subComponent: component.getComponents()){
                    processAllComponents(copyrightsToSave, inventoryItemsToSave, softwareComponentsToSave, subComponent, context, count++);
                }
            }

        } catch (Exception e) {
            log.error("Error retrieving cycloneDx object for component: {}", component.getPurl(), e);
        }


    }

    public InventoryItem parseSinglePackage(Component component, CycloneDxImportContext context, Set<Copyright> copyrightsToSave, SoftwareComponent sc)
            throws Exception {

        List<OrtIssue> ortIssues= ortIssueRepository.findByProject(context.getProject());
        List<OrtViolation> ortViolations = ortViolationRepository.findByProject(context.getProject());

        log.info("Looking at package: {}", component.getPurl());

        String packageName = component.getName();
        String version = component.getVersion();
        List<Copyright> copyrights = new ArrayList<>();

        sc = context.getComponentCache().get(component.getPurl());

        if (sc == null) {
            sc = softwareComponentService.getOrCreateSoftwareComponent(packageName, version, context.getProject().getOrganization());
            context.getComponentCache().put(component.getPurl(), sc);

        }
        //setting this always new for each sbom the value is different, needed later for sorting vulnerabilities etc
        sc.setBomRef(component.getBomRef());
        sc.setPurl(component.getPurl());
        //to handle vulnerabilities later + faster
        context.getComponentVulnerabilityCache().put(component.getBomRef(), sc);
        //get License from package
        LicenseChoice licenseChoices = component.getLicenses();

        String packageLicenseString= licenseHandler.createUsageLicenses(licenseChoices, context,
                sc, context.getProject().getOrganization());


        String inventoryName = sc.getName();
        if (!inventoryName.contains(component.getVersion())) inventoryName +=" "+ component.getVersion();
        inventoryName += " (" + packageLicenseString + ")";

        InventoryItem inventoryItem;
        if(!context.getInventoryCache().containsKey(inventoryName)) {
            inventoryItem = inventoryItemService.getOrCreateInventoryItem(inventoryName, sc,
                    context.getProject(),
                    context.getProject().getOrganization());
            inventoryItem.setCurated(false);
        }else {
            inventoryItem= context.getInventoryCache().get(inventoryName);
        }

        inventoryItemService.sortViolationsAndIssues(ortIssues, ortViolations, inventoryItem);

        if(component.getEvidence().getOccurrences()!= null){
            handleFiles(component, inventoryItem, context);
        }

        if (component.getCopyright() != null || component.getEvidence().getCopyright() != null) {
            List<Copyright> copyrightList= handleCopyrights(component, context, copyrightsToSave);
            sc.setCopyrights(copyrightList);
        } else {
            sc.setCopyrights(new ArrayList<>(copyrights));
        }


        //TODO discuss which type should be used here, we can only save one...
        String vcsUrl = "";
        String sourceUrl="";
        String downloadLocation = "";

        if (component.getExternalReferences() != null) {
            for (ExternalReference ref : component.getExternalReferences()) {

                // 1. Prüfen, ob es sich um ein Version Control System (VCS) handelt
                if (ref.getType() == ExternalReference.Type.VCS) {
                    vcsUrl = ref.getUrl();
                    break;
                }else if(ref.getType()== ExternalReference.Type.SOURCE_DISTRIBUTION){
                    sourceUrl= ref.getUrl();
                }
                    else {
                        downloadLocation = ref.getUrl();
                    }
            }
        }

        if (!vcsUrl.isEmpty()) {
            downloadLocation= vcsUrl;
        }else if(!sourceUrl.isEmpty()) {
            downloadLocation = sourceUrl;
        }

        sc.setDetailsUrl(downloadLocation);

        log.info("created inventoryItem: {}", inventoryName);
        log.info("created softwareComponent: {}", component.getName());

       //TODO main package handling? context.mainpackage
        return inventoryItem;
    }

    private List<Copyright> handleCopyrights(Component component, CycloneDxImportContext context, Set<Copyright> copyrightsToSave){
        Set<String> allCopyrightsTexts = new HashSet<>();
        allCopyrightsTexts.add(component.getCopyright());

        for (org.cyclonedx.model.Copyright copyright : component.getEvidence().getCopyright()) {


                String copyrightText = copyright.getText();
                allCopyrightsTexts.add(copyrightText);
        }
        Map<String, Copyright> copyrightMap = copyrightService.findOrCreateBatch(allCopyrightsTexts,
                context.getProject().getOrganization());


        copyrightsToSave.addAll(copyrightMap.values());
        return new ArrayList<>(copyrightMap.values());
    }

    private void handleFiles(Component component, InventoryItem inventoryItem, CycloneDxImportContext context) throws InvalidSPDXAnalysisException {
        Set<String> allCopyrightsTexts = new HashSet<>();
        allCopyrightsTexts.add(component.getCopyright());

        for(Occurrence occ: component.getEvidence().getOccurrences()) {
            String filepath= occ.getLocation();
            allCopyrightsTexts.add(filepath);
        }

        Map<String, File> locationMap = fileService.findOrCreateBatch(allCopyrightsTexts, inventoryItem);

        Project project= inventoryItem.getProject();
        project.addFiles(new HashSet<>(locationMap.values()));

    }


}