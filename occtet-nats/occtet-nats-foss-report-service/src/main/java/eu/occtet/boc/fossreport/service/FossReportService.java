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

package eu.occtet.boc.fossreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.dao.ImportTaskRepository;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.fossreport.dao.SoftwareComponentRepository;
import eu.occtet.boc.model.*;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.NatsStreamSender;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@Transactional
public class FossReportService extends BaseWorkDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(FossReportService.class);


    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private CodeLocationService codeLocationService;
    @Autowired
    private LicenseService licenseService;
    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private Connection natsConnection;

    @Value("${nats.send-subject1}")
    private String sendSubject1;
    @Value("${nats.send-subject2}")
    private String sendSubject2;
    @Value("${nats.send-subject3}")
    private String sendSubject3;

    private static final String CONFIG_KEY_USE_LICENSE_MATCHER = "UseLicenseMatcher";
    private static final String CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER = "UseFalseCopyrightFilter";

    @Bean
    public NatsStreamSender natsStreamSenderLicenseMatcher(){
        return new NatsStreamSender(natsConnection, sendSubject1);
    }

    @Bean
    public NatsStreamSender natsStreamSenderCopyrightFilter(){
        return new NatsStreamSender(natsConnection, sendSubject2);
    }

    @Bean
    public NatsStreamSender natsStreamSenderVulnerabilityService(){
        return new NatsStreamSender(natsConnection, sendSubject3);
    }

    @Override
    public boolean process(FossReportServiceWorkData workData) {
        log.debug("FossReportService: creates entities to curate from data {}", workData.toString());
        return generateAndStoreData(workData);
    }

    /**
     * Generates and stores data based on the provided MessageDto input.
     * This method processes the input data, prepares the necessary details for inventory items,
     * software components, licenses, and copyrights, and persists the information
     * in the relevant repositories.
     *
     */
    protected boolean generateAndStoreData(FossReportServiceWorkData workData) {
        log.info("Starting generating data...");

        if (workData == null || workData.getRowData() == null) {
            log.error("No data provided!");
        }
        RowDto rowDto;
        InventoryItem inventoryItem;
        try {
            Map<String, Object> rowData = workData.getRowData();
            rowDto = FossReportUtilities.convertMapToRowDto(rowData);
        } catch (Exception e) {
            log.error("Error while processing data: ", e);
            return false;
        }
        Optional<ImportTask> importTask = scannerInitializerRepository.findById(workData.getScannerInitializerId());
        Optional<Project> project;

        if (importTask.isEmpty() ) {
            log.error("Scanner not found! (id:{})", workData.getScannerInitializerId() );
        } else {
            project = projectRepository.findById(importTask.get().getProject().getId());
                    //inventoryItemRepository.findById(importTask.get().getInventoryItem().getId());
            if (project.isEmpty() || rowDto == null) {
                log.error("Project not found! (id:{}) or row is null: {}", importTask.get().getProject().getId(), rowDto== null );
            } else {
                try {
                    // prepare necessary data
                    String inventoryName = rowDto.componentNameAndVersion();
                    log.debug("InventoryName: {}", inventoryName);
                    String componentVersion = FossReportUtilities.extractVersion(inventoryName);
                    String componentName = FossReportUtilities.extractVersionOfComponentName(inventoryName, componentVersion);
                    String cveDictionaryEntry = FossReportUtilities.extractCveDictionaryEntry(rowDto.vulnerabilityList());
                    String severity = FossReportUtilities.extractSeverity(rowDto.vulnerabilityList());
                    boolean wasCombined = FossReportUtilities.wasCombined(inventoryName);
                    boolean isStyleBy = inventoryName.contains("Style");
                    String url= rowDto.URL();
                    List<License> licenses = prepareLicenses(wasCombined, isStyleBy, rowDto);
                    String parentComponentVersion = FossReportUtilities.extractVersion(rowDto.parentNameAndVersion());
                    String parentComponentName = FossReportUtilities.extractVersionOfComponentName(rowDto.parentNameAndVersion(),
                            parentComponentVersion);
                    String parentInventoryName = rowDto.parentNameAndVersion();
                    int priority = rowDto.priority();
                    log.debug("parentInventoryName: {}", parentInventoryName);
                    log.debug("project: {}", project);

                    SoftwareComponent softwareComponent = softwareComponentService.getOrCreateSoftwareComponent(
                            componentName, componentVersion, licenses, url);

                    // Ensure that the parent inventory has a software Component
                    SoftwareComponent parentSoftwareComponent =
                            softwareComponentService.getOrCreateSoftwareComponent(parentComponentName, parentComponentVersion);

                    InventoryItem parentInventory = inventoryItemService.getOrCreateInventoryItem(parentInventoryName, parentSoftwareComponent, project.get());

                    log.debug("parent inventory : {}", parentInventory.getInventoryName());

                    String basePath = FossReportUtilities.determineBasePath(rowDto.files()).trim();
                    log.debug("basePath: {}", basePath);

                    List<Copyright> copyrights = new ArrayList<>();
                    inventoryItem=  inventoryItemService.getOrCreateInventoryItemWithAllAttributes(
                            project.get(), inventoryName, rowDto.size()==null?0:rowDto.size(),
                            rowDto.linking(), rowDto.externalNotes(),
                            parentInventory, softwareComponent, wasCombined, copyrights, priority
                    );
                    CodeLocation basePathCodeLocation = codeLocationService.findOrCreateCodeLocationWithInventory(basePath, inventoryItem);
                    prepareCodeLocations(rowDto, inventoryItem, basePathCodeLocation);
                    //as we have no specific codeLocation for the copyrights here, we just use the basepath
                    copyrights = prepareCopyrights(rowDto, basePathCodeLocation);

                    inventoryItem.getSoftwareComponent().setCopyrights(copyrights);
                    inventoryItemRepository.save(inventoryItem);
                    softwareComponentRepository.save(inventoryItem.getSoftwareComponent());

                    log.info("Finished generating data.");

                    importTaskService.updateImportFeedback(importTask.get(),
                            "Finished processing data and related data for inventory item " + inventoryItem.getInventoryName());

                   sendVulnerbilityToStream(inventoryItem);
                    // send inventory item to next step in workflow
                    ScannerSendWorkData workDataResponse = new ScannerSendWorkData(inventoryItem.getId());
                    sendResultToStream(workDataResponse, importTask.get(), !copyrights.isEmpty());

                } catch (Exception e) {
                    log.error("Exception occurred while processing: {}", e.getMessage(), e);
                    importTaskService.updateImportFeedback(importTask.get(),
                            "Error occured while processing data and related data for inventory item " + rowDto.componentNameAndVersion() + ": " + e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    private void sendVulnerbilityToStream(InventoryItem inventoryItem) throws IOException, JetStreamApiException {

        // send software id to vulnerability microservice
        VulnerabilityServiceWorkData vulnerabilityServiceWorkData =
                new VulnerabilityServiceWorkData(inventoryItem.getSoftwareComponent().getId());
        WorkTask workTask = new WorkTask(
                44,
                "sending software component to vulnerability microservice",
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond(),
                vulnerabilityServiceWorkData);
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(workTask);
        log.info("Sending software id to vulnerability microservice with message: {}", message);
        natsStreamSenderVulnerabilityService().sendWorkMessageToStream(message.getBytes(StandardCharsets.UTF_8));

    }


    /**
     * Helper method to extract license information from the given RowDto object.
     * @return a list of License objects containing extracted license information.
     */
    private List<License> prepareLicenses(boolean wasCombined, Boolean isStyleBy, RowDto rowDto) {

        List<String> licenseNames;
        List<License> licenses = new ArrayList<>();

        if (wasCombined) {
            licenseNames = FossReportUtilities.separateCombinedLicenses(rowDto.licenseTypeId());
            for (String licenseName : licenseNames) {
                licenses.add(licenseService.findOrCreateLicenseWithModified(licenseName, rowDto.licenseText(),
                        isStyleBy));
            }
        } else {
            licenses.add(licenseService.findOrCreateLicenseWithModified(rowDto.licenseTypeId(),
                    rowDto.licenseText(), isStyleBy));
        }
        log.debug("Licenses found: {}", licenses.size());
        return licenses;
    }

    private void prepareCodeLocations(RowDto rowDto, InventoryItem inventoryItem, CodeLocation basePathCodeLocation) {
        log.debug("prepare codeLocations with paths: {}", rowDto.files());

        if (rowDto.files() != null && !rowDto.files().isEmpty()) {
            List<String> filePaths= PathUtilities.cleanAndSplits(rowDto.files());
            codeLocationService.deleteOldCodeLocationsOfInventoryItem(inventoryItem, basePathCodeLocation);
            codeLocationService.CreateCodeLocationsWithInventory(filePaths, inventoryItem);
        }
    }

    private List<Copyright> prepareCopyrights(RowDto rowDto, CodeLocation basePathCodeLocation) {
        log.debug("prepare copyrights with text: {}", rowDto.copyright());
        List<Copyright> copyrights = new ArrayList<>();
        List<String> copyrightTexts = FossReportUtilities.getCopyrights(rowDto.copyright());
        log.debug("CopyrightTexts: {}", copyrightTexts);
        if (!copyrightTexts.isEmpty()) {
            copyrightTexts.forEach(copyrightText -> {
                // For FlexeraReport use the basepath for copyrights, as we dont have a specific path for them.
                Copyright copyright = copyrightService.findOrCreateCopyright(copyrightText, basePathCodeLocation);
                copyrights.add(copyright);
            });
        }
        return copyrights;
    }


    /**
     * Sends the generated InventoryItem to the NATS stream for further processing.
     * @param sendWorkData
     * @throws JetStreamApiException
     * @throws IOException
     */
    private void sendResultToStream(ScannerSendWorkData sendWorkData, ImportTask importTask, boolean hasCopyrights){


        boolean useLicenseMatcher = importTask.getImportConfiguration().stream()
                .filter(c -> CONFIG_KEY_USE_LICENSE_MATCHER.equals(c.getName()))
                .map(c -> Boolean.parseBoolean(c.getValue()))
                .findFirst()
                .orElse(false);

        boolean useCopyrightFilter = importTask.getImportConfiguration().stream()
                .filter(c -> CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER.equals(c.getName()))
                .map(c -> Boolean.parseBoolean(c.getValue()))
                .findFirst()
                .orElse(false);

        log.info("Sending work data to stream, license {} copyright{}",useLicenseMatcher, useCopyrightFilter);
        log.debug("SEND inventoryId {}", sendWorkData.getInventoryItemId());
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask(55, "sending inventoryItem to next microservice according to config", actualTimestamp, sendWorkData);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String message = objectMapper.writeValueAsString(workTask);

        // Get the current date and time
        if (useLicenseMatcher) {
            log.debug("sending message to licenseMatcher service: {}", message);
            natsStreamSenderLicenseMatcher().sendWorkMessageToStream( message.getBytes(Charset.defaultCharset()));
        }
        if (useCopyrightFilter && hasCopyrights) {
            log.info("Sending copyright filtering work data to ai service");
            natsStreamSenderCopyrightFilter().sendWorkMessageToStream(message.getBytes(Charset.defaultCharset()));
        } else {
            log.info("No more work to do");
        }
        } catch (Exception e) {
        log.debug("Error with foss service connection: " + e.getMessage());
    }
    }

}
