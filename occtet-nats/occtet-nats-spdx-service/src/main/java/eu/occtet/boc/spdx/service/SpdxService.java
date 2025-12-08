/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.spdx.service;



import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.spdx.dao.InventoryItemRepository;
import eu.occtet.boc.spdx.dao.LicenseRepository;
import eu.occtet.boc.spdx.dao.ProjectRepository;
import io.nats.client.JetStreamApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.TypedValue;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.*;
import org.spdx.library.model.v2.license.*;
import org.spdx.library.model.v3_0_1.expandedlicensing.ListedLicense;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpdxService extends BaseWorkDataProcessor{

    private static final Logger log = LogManager.getLogger(SpdxService.class);

    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private LicenseService licenseService;
    @Autowired
    private CodeLocationService codeLocationService;
    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private AnswerService answerService;



    private Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc = new ArrayList<>();

    @Override
    public boolean process(SpdxWorkData workData) {
        log.debug("SpdxService: reads spdx and creates entities to curate {}", workData.toString());
        return parseDocument(workData);
    }

    /**
     * Takes spdxWorkData, extracts contained json and creates entities based on the deserialized spdxDocument created from the json.
     * If the entities are already present then no new ones will be created, however some of their attributes may change.
     * @param spdxWorkData
     * @return true if the entities where created successfully, false is any error occurred
     */
    public boolean parseDocument(SpdxWorkData spdxWorkData){
        try {

            // setup for spdx library need to be called once before any spdx model objects are accessed
            SpdxModelFactory.init();
            InMemSpdxStore modelStore = new InMemSpdxStore();
            MultiFormatStore inputStore = new MultiFormatStore(modelStore, MultiFormatStore.Format.JSON);

            byte[] spdxJsonBytes = spdxWorkData.getJsonBytes();

            InputStream inputStream = new ByteArrayInputStream(spdxJsonBytes);
            SpdxDocument spdxDocument = inputStore.deSerialize(inputStream, false);

            UUID projectId = UUID.fromString(spdxWorkData.getProjectId());
            Optional<Project> projectOptional = projectRepository.findById(projectId);
            if(projectOptional.isEmpty()) {
               log.error("failed to find the project");
                return false;
            }
            Project project = projectOptional.get();


            List<InventoryItem> inventoryItems = new ArrayList<>();
            List<TypedValue> packageUri = spdxDocument.getModelStore().getAllItems(null, "Package").toList();
            List<SpdxPackage> spdxPackages = new ArrayList<>();
            //avoid looking at packages multiple times
            HashSet<String> seenPackages = new HashSet<>();
            licenseInfosExtractedSpdxDoc = spdxDocument.getExtractedLicenseInfos();

            //get the list of described packages for the download-service to differentiate between them and dependencies
            Set<String> mainPackageIds = new HashSet<>();
            try {
                Set<String> ids = spdxDocument.getDocumentDescribes().stream()
                        .map(SpdxElement::getId)
                        .collect(Collectors.toSet());
                mainPackageIds.addAll(ids);
            } catch (InvalidSPDXAnalysisException e) {
                log.warn("Could not read DocumentDescribes: {}", e.getMessage());
            }

            for (TypedValue uri : packageUri) {
                SpdxModelFactory.getSpdxObjects(spdxDocument.getModelStore(), null, "Package", uri.getObjectUri(), null).forEach(
                        spdxPackage -> {
                            try {
                                if (!seenPackages.contains(spdxPackage.toString())) {
                                    inventoryItems.add(parsePackages((SpdxPackage) spdxPackage, project, mainPackageIds));
                                    spdxPackages.add((SpdxPackage) spdxPackage);
                                    seenPackages.add((spdxPackage).toString());
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }
            for (SpdxPackage spdxPackage : spdxPackages) {
                parseRelationships(spdxPackage, spdxPackage.getRelationships().stream().toList(), project);
            }

            log.info("processed spdx with {} items", inventoryItems.size());
            boolean sent = answerService.prepareAnswers(inventoryItems, spdxWorkData.isUseCopyrightAi(), spdxWorkData.isUseLicenseMatcher());
            log.info(("SENT"));
            return sent;

        }catch (InvalidSPDXAnalysisException | IOException | JetStreamApiException e ){
            log.error("An error occurred while trying to deserialize spdx: {}", e.toString());
            return false;
        }
    }

    private InventoryItem parsePackages(SpdxPackage spdxPackage, Project project, Set<String> mainPackageIds)
            throws Exception {

        log.info("Looking at package: {}", spdxPackage.getId());

        String packageName = spdxPackage.getName().orElse(spdxPackage.getId());
        List<CodeLocation> codeLocations = new ArrayList<>();
        List<Copyright> copyrights = new ArrayList<>();

        // Creation
        // Use the method from service instead of factory to avoid duplicated software components
        SoftwareComponent component = softwareComponentService.getOrCreateSoftwareComponent(packageName, spdxPackage.getVersionInfo().orElse(""));

        //get License from package
        AnyLicenseInfo spdxPkgLicense = spdxPackage.getLicenseConcluded();
        if (spdxPkgLicense.isNoAssertion(spdxPkgLicense)) {
            spdxPkgLicense = spdxPackage.getLicenseDeclared();
        }

        List<License> pkgLicenses = createLicenses(spdxPkgLicense);
        if(component.getLicenses() != null) {
            //make double sure there are no doubles
            Set<License> lSet= new HashSet<>( component.getLicenses());
            component.setLicenses(new ArrayList<>(lSet));
            pkgLicenses.stream()
                    .filter(license -> !component.getLicenses().contains(license))
                    .forEach(component::addLicense);
        } else {
            component.setLicenses(pkgLicenses);
        }

        String packageLicenseString = spdxPkgLicense != null ? spdxPkgLicense.toString() : "";

        String inventoryName = spdxPackage.getId().replaceAll("(?i)^SPDXRef-[^-]+-[^-]+-", "");
        if (!inventoryName.contains(component.getVersion())) inventoryName += component.getVersion();
        inventoryName += " (" + packageLicenseString + ")";

        //Check if license of component is combined
        Pattern pattern = Pattern.compile("\\b(?:BUT|AND)\\b");
        boolean isCombined = pattern.matcher(packageLicenseString).find();

        InventoryItem inventoryItem = inventoryItemService.getOrCreateInventoryItem(inventoryName, component, project);
        inventoryItem.setWasCombined(isCombined);
        inventoryItem.setSpdxId(spdxPackage.getId());
        inventoryItem.setCurated(false);

        inventoryItem.setSize(spdxPackage.getFiles().size());

        spdxPackage.getFiles().forEach(f -> {
            try {
                parseFiles(f, inventoryItem, codeLocations, copyrights);
            } catch (InvalidSPDXAnalysisException e) {
                throw new RuntimeException(e);
            }
        });


        if (component.getCopyrights() == null){
            component.setCopyrights(copyrights);
        }else{
            Set<Copyright> uniqueCopyrights = new HashSet<>();
            uniqueCopyrights.addAll(component.getCopyrights());
            uniqueCopyrights.addAll(copyrights);
            component.setCopyrights(new ArrayList<>(uniqueCopyrights));
        }


        String downloadLocation = spdxPackage.getDownloadLocation().orElse("");
        component.setDetailsUrl(downloadLocation);

        List<ExternalRef> externalRefs = spdxPackage.getExternalRefs().stream().toList();
        for(ExternalRef externalRef: externalRefs){
            if(externalRef.getReferenceType().getIndividualURI().endsWith("purl")){
                component.setPurl(externalRef.getReferenceLocator());
                log.info("Found purl: {} for Component: {}", externalRef.getReferenceLocator(), component.getName());
            }
        }

        String version = spdxPackage.getVersionInfo().orElse("");
        if (!version.isEmpty() && !downloadLocation.isEmpty()) {
            if(answerService.sendToDownload(downloadLocation ,project.getBasePath(), version,
                    project.getId().toString(), mainPackageIds.contains(spdxPackage.getId()))){
                log.info("sending to DownloadService was successful");
            }else{
                log.error("failed to send to Downloadservice");
            }
        }

        softwareComponentService.update(component);
        inventoryItemService.update(inventoryItem);
        log.info("created inventoryItem: {}", inventoryName);
        log.info("created softwareComponent: {}", component.getName());

        return inventoryItem;
    }

    private List<License> createLicenses(AnyLicenseInfo spdxLicenseInfo) throws InvalidSPDXAnalysisException {

        Set<License> allLicenses = new HashSet<>();
        List<AnyLicenseInfo> allLicenseInfo = new ArrayList<>();
        parseLicenseText(spdxLicenseInfo, allLicenseInfo);

        for (AnyLicenseInfo individualLicenseInfo : allLicenseInfo) {
            if (individualLicenseInfo instanceof SpdxListedLicense) {
                ListedLicense license = LicenseInfoFactory.getListedLicenseById(individualLicenseInfo.getId());
                String licenseId = license.getId();
                if(licenseId.isEmpty()){
                    licenseId= "Unknown";
                }
                log.debug("adding license {}", licenseId);
                String licenseText = license.getLicenseText();
                License licenseEntity = licenseService.findOrCreateLicense(licenseId, licenseText, licenseId);
                licenseEntity.setSpdx(true);
                //save changes to spdx status
                licenseRepository.save(licenseEntity);
                allLicenses.add(licenseEntity);
            } else if (individualLicenseInfo instanceof ExtractedLicenseInfo) {
                Optional<ExtractedLicenseInfo> extractedLicense = licenseInfosExtractedSpdxDoc.stream().filter(s -> s.getLicenseId().equals(individualLicenseInfo.getId())).findFirst();
                if (extractedLicense.isPresent()) {
                    String licenseId = extractedLicense.get().getId();
                    if(licenseId.isEmpty()){
                        licenseId= "Unknown";
                    }
                    log.debug("adding license {}", licenseId);
                    String licenseText = extractedLicense.get().getExtractedText();
                    allLicenses.add(licenseService.findOrCreateLicense(licenseId, licenseText, licenseId));
                }
            }
        }

        return allLicenses.stream().toList();
    }

    private void parseLicenseText(AnyLicenseInfo licenseInfo, List<AnyLicenseInfo> allLicenseInfos) throws InvalidSPDXAnalysisException {
        switch (licenseInfo) {
            case ConjunctiveLicenseSet conjunctiveLicenseSet -> {
                for (AnyLicenseInfo member : conjunctiveLicenseSet.getMembers()) {
                    parseLicenseText(member, allLicenseInfos);
                }
            }
            case DisjunctiveLicenseSet disjunctiveLicenseSet -> {
                for (AnyLicenseInfo member : disjunctiveLicenseSet.getMembers()) {
                    parseLicenseText(member, allLicenseInfos);
                }
            }
            case WithExceptionOperator withExceptionOperator -> parseLicenseText(withExceptionOperator.getLicense(), allLicenseInfos);
            case SpdxListedLicense listed -> allLicenseInfos.add(listed);
            case ExtractedLicenseInfo extracted -> allLicenseInfos.add(extracted);
            //No action needed if there is no license
            case SpdxNoneLicense ignored -> {
            }
            case null, default -> log.info("Encountered unknown license type: {}", licenseInfo);
        }
    }

    private void parseFiles(SpdxFile spdxFile, InventoryItem inventoryItem, List<CodeLocation> codeLocations, List<Copyright> copyrights) throws InvalidSPDXAnalysisException {
        String copyrightText = spdxFile.getCopyrightText();
        if (!copyrightText.equals("NONE") && spdxFile.getName().isPresent()) {
            CodeLocation fileLocation =
                    codeLocationService.findOrCreateCodeLocationWithInventory(spdxFile.getName().get(), inventoryItem);
            if (!codeLocations.contains(fileLocation)) {
                codeLocations.add(fileLocation);
            }
            Copyright fileCopyright = copyrightService.findOrCreateCopyright(copyrightText, fileLocation);
            if (!copyrights.contains(fileCopyright)) {
                copyrights.add(fileCopyright);
                log.info("Created codeLocation: {} for Copyright: {}", fileLocation.getFilePath(), copyrightText);
            }
        }
    }

    private void parseRelationships(SpdxPackage spdxPackage, List<Relationship> relationships, Project project) throws InvalidSPDXAnalysisException {
        for (Relationship relationship : relationships) {
            switch (relationship.getRelationshipType()) {
                case CONTAINS, DEPENDS_ON, ANCESTOR_OF -> {
                    Optional<SpdxElement> target = relationship.getRelatedSpdxElement();
                    if (target.isPresent() && target.get().getType().equals("Package")) {
                        InventoryItem sourceItem = inventoryItemRepository.findBySpdxIdAndProject(spdxPackage.getId(), project).getFirst();
                        InventoryItem targetItem = inventoryItemRepository.findBySpdxIdAndProject(target.get().getId(), project).getFirst();
                        targetItem.setParent(sourceItem);
                        inventoryItemService.update(targetItem);
                        log.info("identified {} as parent of {}", sourceItem.getInventoryName(), targetItem.getInventoryName());
                        log.debug("child has project {}", targetItem.getProject().getProjectName());
                    }
                }
                case CONTAINED_BY, DEPENDENCY_OF, DESCENDANT_OF -> {
                    Optional<SpdxElement> target = relationship.getRelatedSpdxElement();
                    if (target.isPresent() && target.get().getType().equals("Package")) {
                        InventoryItem sourceItem = inventoryItemRepository.findBySpdxIdAndProject(spdxPackage.getId(), project).getFirst();
                        InventoryItem targetItem = inventoryItemRepository.findBySpdxIdAndProject(target.get().getId(), project).getFirst();
                        sourceItem.setParent(targetItem);
                        inventoryItemService.update(sourceItem);
                        log.info("identified {} as child of {}", sourceItem.getInventoryName(), targetItem.getInventoryName());
                    }
                }
                case STATIC_LINK -> {
                    Optional<SpdxElement> target = relationship.getRelatedSpdxElement();
                    if (target.isPresent() && target.get().getType().equals("Package")) {
                        InventoryItem targetItem = inventoryItemRepository.findBySpdxIdAndProject(target.get().getId(), project).getFirst();
                        targetItem.setLinking("Static");
                        inventoryItemService.update(targetItem);
                    }
                }
                case DYNAMIC_LINK -> {
                    Optional<SpdxElement> target = relationship.getRelatedSpdxElement();
                    if (target.isPresent() && target.get().getType().equals("Package")) {
                        InventoryItem targetItem = inventoryItemRepository.findBySpdxIdAndProject(target.get().getId(), project).getFirst();
                        targetItem.setLinking("Dynamic");
                        inventoryItemService.update(targetItem);
                    }
                }
                case null, default -> {
                }
            }
        }
    }



}