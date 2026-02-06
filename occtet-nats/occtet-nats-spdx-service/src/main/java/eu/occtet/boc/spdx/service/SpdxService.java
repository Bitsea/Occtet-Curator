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


import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.service.ProgressReportingService;
import eu.occtet.boc.spdx.converter.SpdxConverter;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class SpdxService extends ProgressReportingService  {

    private static final Logger log = LogManager.getLogger(SpdxService.class);

    @Autowired
    SpdxConverter spdxConverter;
    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private LicenseService licenseService;
    @Autowired
    private FileService fileService;
    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private SpdxDocumentRootRepository spdxDocumentRootRepository;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private CleanUpService cleanUpService;

    private Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc = new ArrayList<>();

    public boolean process(SpdxWorkData workData) {
        log.debug("SpdxService: reads spdx and creates entities to curate {}", workData.toString());
        return parseDocument(workData);
    }

    /**
     * Takes spdxWorkData, extracts contained JSON and creates entities based on the deserialized spdxDocument created from the JSON.
     * If the entities are already present then no new ones will be created, however some of their attributes may change.
     *
     * @param spdxWorkData
     * @return true if the entities where created successfully, false is any error occurred
     */
    public boolean parseDocument(SpdxWorkData spdxWorkData){
        try {
            log.info("now processing spdx for project id: {}", spdxWorkData.getProjectId());
            notifyProgress(1,"init");
            // setup for spdx library need to be called once before any spdx model objects are accessed
            SpdxModelFactory.init();
            InMemSpdxStore modelStore = new InMemSpdxStore();
            MultiFormatStore inputStore = new MultiFormatStore(modelStore, MultiFormatStore.Format.JSON);

            byte[] spdxJsonBytes = spdxWorkData.getJsonBytes();

            InputStream inputStream = new ByteArrayInputStream(spdxJsonBytes);
            SpdxDocument spdxDocument = inputStore.deSerialize(inputStream, false);

            SpdxDocumentRoot spdxDocumentRoot = spdxConverter.convertSpdxV2DocumentInformation(spdxDocument);
            notifyProgress(10,"converting spdx");
            long projectId = spdxWorkData.getProjectId();
            Optional<Project> projectOptional = projectRepository.findById(projectId);
            if(projectOptional.isEmpty()) {
               log.error("failed to find the project");
                return false;
            }
            Project project = projectOptional.get();
            project.setDocumentID(spdxDocument.getDocumentUri());
            projectRepository.save(project);

            List<InventoryItem> inventoryItems = new ArrayList<>();
            List<TypedValue> packageUri = spdxDocument.getModelStore().getAllItems(null, "Package").toList();
            List<SpdxPackage> spdxPackages = new ArrayList<>();
            //avoid looking at packages multiple times
            HashSet<String> seenPackages = new HashSet<>();
            licenseInfosExtractedSpdxDoc = spdxDocument.getExtractedLicenseInfos();
            Set<String> processedFileIds = new HashSet<>();

            //get the list of described packages for the download-service to differentiate between them and dependencies
            Set<String> mainPackageIds = new HashSet<>();
            Set<Long> mainInventoryItems = new HashSet<>();
            notifyProgress(20,"collecting document describes");
            try {
                Set<String> ids = spdxDocument.getDocumentDescribes().stream()
                        .map(SpdxElement::getId)
                        .collect(Collectors.toSet());
                mainPackageIds.addAll(ids);
            } catch (InvalidSPDXAnalysisException e) {
                log.warn("Could not read DocumentDescribes: {}", e.getMessage());
            }
            //before entities are created and saved a cleanup for file entities connected to the project is done
            cleanUpService.cleanUpFileTree(project);

            Map<String, SpdxPackageEntity> packageLookupMap = new HashMap<>();
            if (spdxDocumentRoot.getPackages() != null) {
                for (SpdxPackageEntity entity : spdxDocumentRoot.getPackages()) {
                    if (entity.getSpdxId() != null) {
                        packageLookupMap.put(entity.getSpdxId(), entity);
                    }
                }
            }

            //Caches for component and license
            Map<String, SoftwareComponent> componentCache = new HashMap<>();
            Map<String, License> licenseCache = new HashMap<>();
            Map<String, InventoryItem> fileToInventoryItemMap = new HashMap<>();
            int count=0;
            for (TypedValue uri : packageUri) {
                SpdxModelFactory.getSpdxObjects(spdxDocument.getModelStore(), null, "Package", uri.getObjectUri(), null).forEach(
                        spdxPackage -> {
                            try {
                                if (!seenPackages.contains(spdxPackage.toString())) {
                                    log.debug("Processing unseen package: {}", spdxPackage.toString());
                                    inventoryItems.add(parsePackages((SpdxPackage) spdxPackage, project,
                                            spdxDocumentRoot, packageLookupMap, componentCache, licenseCache,
                                            mainInventoryItems, mainPackageIds, fileToInventoryItemMap,
                                            processedFileIds));
                                    spdxPackages.add((SpdxPackage) spdxPackage);
                                    spdxPackages.add((SpdxPackage) spdxPackage);
                                    seenPackages.add((spdxPackage).toString());
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                count++;
                int progress = 20 + (int) ( (40*count) / packageUri.size());
                if(progress%5 ==0)
                    notifyProgress(progress, "processing packages");
            }

            List<InventoryItem> allItems = inventoryItemRepository.findAllByProject(project);
            Map<String, InventoryItem> inventoryCache = allItems.stream()
                    .filter(i -> i.getSpdxId() != null)
                    .collect(Collectors.toMap(InventoryItem::getSpdxId, item -> item, (p1, p2) -> p1));

            try {
                processOrphanFiles(spdxDocument, project, spdxDocumentRoot,
                        licenseCache, fileToInventoryItemMap, processedFileIds, inventoryItems);
            } catch (Exception e) {
                log.error("Error processing orphan files", e);
            }

            count=0;
            for (SpdxPackage spdxPackage : spdxPackages) {
                parseRelationships(spdxPackage, spdxPackage.getRelationships().stream().toList(), inventoryCache);
                for(Relationship relationship: spdxPackage.getRelationships()) {
                    spdxConverter.convertRelationShip(relationship, spdxDocumentRoot, spdxPackage);
                }
                log.debug("Converted {} relationships for package {}", spdxPackage.getRelationships().size(), spdxPackage.getId());
                count++;
                int progress = 60 + (int) (((double) count / packageUri.size()) * 40);
                if(progress%5 ==0)
                    notifyProgress(progress, "converting relationships");
            }

            Stream<?> rawStream = SpdxModelFactory.getSpdxObjects(
                    spdxDocument.getModelStore(),
                    spdxDocument.getCopyManager(),
                    SpdxConstantsCompatV2.CLASS_SPDX_SNIPPET,
                    spdxDocument.getDocumentUri(),
                    null
            );

            Stream<SpdxSnippet> snippetStream = rawStream
                    .filter(obj -> obj instanceof SpdxSnippet)
                    .map(obj -> (SpdxSnippet) obj);

            snippetStream.forEach(snippet -> {
                //create conversion objects
                spdxConverter.convertSnippets(snippet, spdxDocumentRoot);

                try {
                    enrichComponentFromSnippet(snippet, fileToInventoryItemMap, licenseCache);
                } catch (Exception e) {
                    log.error("Failed to enrich component from snippet: {}", snippet.getId(), e);
                }
            });

            spdxDocumentRootRepository.save(spdxDocumentRoot);

            log.info("processed spdx with {} items", inventoryItems.size());



            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                        log.debug("Transaction committed, sending answers service");
                    try {
                        answerService.prepareAnswers(
                                inventoryItems,
                                spdxWorkData.isUseCopyrightAi(),
                                spdxWorkData.isUseLicenseMatcher(),
                                mainInventoryItems
                        );
                    } catch (Exception e) {
                        log.error("Error sending answers to the answers service", e);
                    }
                    log.debug(("SENT"));
                    }
            });

            notifyProgress(100, "completed");
            return true;

        }catch (InvalidSPDXAnalysisException | IOException e ){
            log.error("An error occurred while trying to deserialize spdx: {}", e.toString());
            return false;
        }
    }



    private InventoryItem parsePackages(SpdxPackage spdxPackage, Project project,
                                        SpdxDocumentRoot spdxDocumentRoot,
                                        Map<String, SpdxPackageEntity> packageLookupMap,
                                        Map<String, SoftwareComponent> componentCache,
                                        Map<String, License> licenseCache, Set<Long> mainInvetoryItems,
                                        Set<String> mainPackageIds,
                                        Map<String, InventoryItem> fileToInventoryItemMap,
                                        Set<String> processedFileIds)
            throws Exception {

        log.info("Looking at package: {}", spdxPackage.getId());
        //Convert to entities
        spdxConverter.convertPackage(spdxPackage, spdxDocumentRoot, packageLookupMap);

        String packageName = spdxPackage.getName().orElse(spdxPackage.getId());
        String version = spdxPackage.getVersionInfo().orElse("");
        List<Copyright> copyrights = new ArrayList<>();

        String componentKey = packageName + ":" + version;
        SoftwareComponent component = componentCache.get(componentKey);

        if (component == null) {
            // Cache Miss: Hit the DB
            component = softwareComponentService.getOrCreateSoftwareComponent(packageName, version);
            // Cache Put
            componentCache.put(componentKey, component);
        }

        //get License from package
        AnyLicenseInfo spdxPkgLicense = spdxPackage.getLicenseConcluded();
        if (spdxPkgLicense.isNoAssertion(spdxPkgLicense)) {
            spdxPkgLicense = spdxPackage.getLicenseDeclared();
        }

        List<License> pkgLicenses = createLicenses(spdxPkgLicense, licenseCache);
        if(component.getLicenses() != null) {
            //make double sure there are no doubles
            Set<License> lSet= new HashSet<>( component.getLicenses());
            component.setLicenses(new ArrayList<>(lSet));
            SoftwareComponent finalComponent = component;
            pkgLicenses.stream()
                    .filter(license -> !finalComponent.getLicenses().contains(license))
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

        log.info("Converting {} files", spdxPackage.getFiles().size());
        spdxPackage.getFiles().forEach(f -> {
            spdxConverter.convertFile(f, spdxDocumentRoot);
            fileToInventoryItemMap.put(f.getId(), inventoryItem);
            processedFileIds.add(f.getId());
        });

        try {
            copyrights = parseFiles(spdxPackage, inventoryItem);
        } catch (InvalidSPDXAnalysisException e) {
            log.error("Error batch processing files", e);
        }


        if (component.getCopyrights() == null){
            component.setCopyrights(new ArrayList<>(copyrights)); // Copy to new list
        } else {
            Set<Copyright> uniqueCopyrights = new HashSet<>(component.getCopyrights());
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

        inventoryItemService.update(inventoryItem);
        log.info("created inventoryItem: {}", inventoryName);
        log.info("created softwareComponent: {}", component.getName());

        if (mainPackageIds.contains(spdxPackage.getId())) {
            mainInvetoryItems.add(inventoryItem.getId());
        }

        return inventoryItem;
    }

    private List<License> createLicenses(AnyLicenseInfo spdxLicenseInfo, Map<String, License> licenseCache)
            throws InvalidSPDXAnalysisException {

        Set<License> allLicenses = new HashSet<>();
        List<AnyLicenseInfo> allLicenseInfo = new ArrayList<>();
        parseLicenseText(spdxLicenseInfo, allLicenseInfo);

        for (AnyLicenseInfo individualLicenseInfo : allLicenseInfo) {
            String licenseId = "";
            String licenseText = "";
            boolean isListed = false;

            if (individualLicenseInfo instanceof SpdxListedLicense) {
                ListedLicense license = LicenseInfoFactory.getListedLicenseById(individualLicenseInfo.getId());
                licenseId = license.getId();
                licenseText = license.getLicenseText();
                isListed = true;
            } else if (individualLicenseInfo instanceof ExtractedLicenseInfo) {
                Optional<ExtractedLicenseInfo> extracted = licenseInfosExtractedSpdxDoc.stream()
                        .filter(s -> s.getLicenseId().equals(individualLicenseInfo.getId())).findFirst();
                if (extracted.isPresent()) {
                    licenseId = extracted.get().getId();
                    licenseText = extracted.get().getExtractedText();
                }
            }

            if (licenseId.isEmpty()) licenseId = "Unknown";

            License licenseEntity = licenseCache.get(licenseId);

            if (licenseEntity == null) {
                licenseEntity = licenseService.findOrCreateLicense(licenseId, licenseText, licenseId);

                if (isListed) {
                    licenseEntity.setSpdx(true);
                    licenseRepository.save(licenseEntity);
                }

                // Cache Put
                licenseCache.put(licenseId, licenseEntity);
            }

            allLicenses.add(licenseEntity);
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

    private List<Copyright> parseFiles(SpdxPackage spdxPackage, InventoryItem inventoryItem) throws InvalidSPDXAnalysisException {
        List<String> allFileNames = new ArrayList<>();
        Set<String> allCopyrightsTexts = new HashSet<>();
        Map<String, String> fileToCopyrightMap = new HashMap<>();
        for (SpdxFile f : spdxPackage.getFiles()) {
            if (f.getName().isPresent()){
                String path = f.getName().get();
                allFileNames.add(path);
                String copyright = f.getCopyrightText();
                if (!"NONE".equals(copyright) && !"NOASSERTION".equals(copyright)){
                    allCopyrightsTexts.add(copyright);
                    fileToCopyrightMap.put(path, copyright);
                }
            }
        }


        Map<String, File> locationMap = fileService.findOrCreateBatch(allFileNames, inventoryItem);
        Map<String, Copyright> copyrightMap = copyrightService.findOrCreateBatch(allCopyrightsTexts);

        List<Copyright> copyrightsToUpdate = new ArrayList<>();
        for (Map.Entry<String, String> entry : fileToCopyrightMap.entrySet()) {
            String path = entry.getKey();
            String copyrightText = entry.getValue();
            File loc = locationMap.get(path);
            Copyright copyright = copyrightMap.get(copyrightText);
            if (loc != null && copyright != null) {
                log.debug("Associating copyright '{}' with file '{}'", copyrightText, path);
                copyright.getFiles().add(loc);
                copyrightsToUpdate.add(copyright);
            }
        }
        copyrightRepository.saveAll(copyrightsToUpdate);

        return new ArrayList<>(copyrightMap.values());
    }

    private void parseRelationships(SpdxPackage spdxPackage, List<Relationship> relationships
            , Map<String, InventoryItem> inventoryCache) throws InvalidSPDXAnalysisException {

        InventoryItem sourceItem = inventoryCache.get(spdxPackage.getId());

        if (sourceItem == null) {
            log.error("Relationship source package not found in inventory: {}", spdxPackage.getId());
            return;
        }

        for (Relationship relationship : relationships) {
            Optional<SpdxElement> targetOpt = relationship.getRelatedSpdxElement();
            if (targetOpt.isEmpty()) continue;

            SpdxElement targetElement = targetOpt.get();

            InventoryItem targetItem = inventoryCache.get(targetElement.getId());

            if (targetItem == null) {
                continue;
            }

            switch (relationship.getRelationshipType()) {
                case CONTAINS, DEPENDS_ON, ANCESTOR_OF -> {
                    if (targetElement.getType().equals("Package")) {
                        targetItem.setParent(sourceItem);
                        inventoryItemService.update(targetItem);
                        log.info("identified {} as parent of {}", sourceItem.getInventoryName(), targetItem.getInventoryName());
                    }
                }
                case CONTAINED_BY, DEPENDENCY_OF, DESCENDANT_OF -> {
                    if (targetElement.getType().equals("Package")) {
                        sourceItem.setParent(targetItem);
                        inventoryItemService.update(sourceItem);
                        log.info("identified {} as child of {}", sourceItem.getInventoryName(), targetItem.getInventoryName());
                    }
                }
                case STATIC_LINK -> {
                    if (targetElement.getType().equals("Package")) {
                        targetItem.setLinking("Static");
                        inventoryItemService.update(targetItem);
                    }
                }
                case DYNAMIC_LINK -> {
                    if (targetElement.getType().equals("Package")) {
                        targetItem.setLinking("Dynamic");
                        inventoryItemService.update(targetItem);
                    }
                }
                case null, default -> {
                }
            }
        }

    }

    private void enrichComponentFromSnippet(SpdxSnippet snippet,
                                            Map<String, InventoryItem> fileMap,
                                            Map<String, License> licenseCache) throws InvalidSPDXAnalysisException {

        SpdxFile snippetFile = snippet.getSnippetFromFile();
        if (snippetFile == null) return;

        InventoryItem item = fileMap.get(snippetFile.getId());
        if (item == null || item.getSoftwareComponent() == null) {
            return;
        }

        SoftwareComponent component = item.getSoftwareComponent();
        boolean componentUpdated = false;

        String snippetCopyright = snippet.getCopyrightText();
        if (snippetCopyright != null && !snippetCopyright.isEmpty()
                && !"NOASSERTION".equals(snippetCopyright) && !"NONE".equals(snippetCopyright)) {

            boolean exists = component.getCopyrights() != null && component.getCopyrights().stream()
                    .anyMatch(c -> c.getCopyrightText().equals(snippetCopyright));

            if (!exists) {
                Copyright copyright = copyrightService.findOrCreateBatch(Set.of(snippetCopyright)).get(snippetCopyright);
                if (copyright != null) {
                    component.getCopyrights().add(copyright);
                    componentUpdated = true;
                }
            }
        }

        AnyLicenseInfo concluded = snippet.getLicenseConcluded();
        if (concluded != null && !concluded.isNoAssertion(concluded) && !concluded.isNoAssertion(concluded)) {
            List<License> licenses = createLicenses(concluded, licenseCache);
            for (License license : licenses) {
                if (!component.getLicenses().contains(license)) {
                    component.addLicense(license);
                    componentUpdated = true;
                }
            }
        }

        if (componentUpdated) {
            softwareComponentService.update(component);
        }
    }

    private void processOrphanFiles(SpdxDocument spdxDocument, Project project,
                                    SpdxDocumentRoot spdxDocumentRoot,
                                    Map<String, License> licenseCache,
                                    Map<String, InventoryItem> fileToInventoryItemMap,
                                    Set<String> processedFileIds,
                                    List<InventoryItem> inventoryItems) throws Exception {

        List<TypedValue> allFileUris = spdxDocument.getModelStore().getAllItems(null, "File").toList();
        List<SpdxFile> orphanFiles = new ArrayList<>();

        for (TypedValue uri : allFileUris) {
            SpdxModelFactory.getSpdxObjects(
                    spdxDocument.getModelStore(),
                    spdxDocument.getCopyManager(),
                    "File",
                    uri.getObjectUri(),
                    null
            ).forEach(obj -> {
                if (obj instanceof SpdxFile file) {
                    if (!processedFileIds.contains(file.getId())) {
                        orphanFiles.add(file);
                    }
                }
            });
        }

        if (orphanFiles.isEmpty()) {
            return;
        }

        log.info("Found {} orphan files. Creating individual inventory items for each.", orphanFiles.size());


        for (SpdxFile file : orphanFiles) {

            String filePath = file.getName().orElse("Unknown File");

            SoftwareComponent component = softwareComponentService.getOrCreateSoftwareComponent(filePath, "Standalone");


            InventoryItem inventoryItem = inventoryItemService.getOrCreateInventoryItem(filePath, component, project);
            inventoryItem.setSpdxId(file.getId());
            inventoryItem.setCurated(false);
            inventoryItem.setSize(1);


            spdxConverter.convertFile(file, spdxDocumentRoot);
            fileToInventoryItemMap.put(file.getId(), inventoryItem);

            Map<String, File> locationMap = fileService.findOrCreateBatch(Collections.singletonList(filePath), inventoryItem);
            File dbFile = locationMap.get(filePath);

            String copyrightText = file.getCopyrightText();
            if (copyrightText != null && !"NONE".equals(copyrightText) && !"NOASSERTION".equals(copyrightText)) {

                Map<String, Copyright> createdCopyrights = copyrightService.findOrCreateBatch(Collections.singleton(copyrightText));
                Copyright copyright = createdCopyrights.get(copyrightText);

                if (copyright != null) {
                    if (dbFile != null) {
                        copyright.getFiles().add(dbFile);
                        copyrightRepository.save(copyright);
                    }

                    if (component.getCopyrights() == null) {
                        component.setCopyrights(new ArrayList<>());
                    }
                    if (!component.getCopyrights().contains(copyright)) {
                        component.getCopyrights().add(copyright);
                    }
                }
            }

            AnyLicenseInfo fileLicense = file.getLicenseConcluded();
            if (fileLicense.isNoAssertion(fileLicense)) {
                fileLicense = file.getLicenseInfoFromFiles().stream().findFirst().orElse(null);
            }

            if (fileLicense != null) {
                List<License> licenses = createLicenses(fileLicense, licenseCache);

                if (component.getLicenses() == null) {
                    component.setLicenses(new ArrayList<>());
                }

                for (License l : licenses) {
                    if (!component.getLicenses().contains(l)) {
                        component.addLicense(l);
                    }
                }
            }

            softwareComponentService.update(component);
            inventoryItemService.update(inventoryItem);
            inventoryItems.add(inventoryItem);
        }
    }

}