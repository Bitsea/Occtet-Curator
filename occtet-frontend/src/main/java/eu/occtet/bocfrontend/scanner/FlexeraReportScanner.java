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

package eu.occtet.bocfrontend.scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import eu.occtet.boc.model.FossReportServiceWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.controller.FlexeraReportController;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.factory.ScannerInitializerFactory;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.service.ScannerInitializerService;
import io.jmix.core.DataManager;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;


@Service
public class FlexeraReportScanner extends Scanner{

    private static final Logger log = LogManager.getLogger(FlexeraReportScanner.class);
    private final ScannerInitializerService scannerInitializerService;


    @Autowired
    private NatsService natsService;

    @Autowired
    private ScannerInitializerFactory scannerInitializerFactory;

    protected FlexeraReportScanner(ScannerInitializerService scannerInitializerService) {
        super("Flexera_Report_Scanner");
        this.scannerInitializerService = scannerInitializerService;
    }

    private static final String CONFIG_KEY_USE_LICENSE_MATCHER = "UseLicenseMatcher";
    private static final String CONFIG_KEY_FILENAME= "fileName";
    private static final String CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER = "UseFalseCopyrightFilter";
    private static final boolean DEFAULT_USE_LICENSE_MATCHER = true;
    private static final boolean DEFAULT_USE_FALSE_COPYRIGHT_FILTER = true;


    @Override
    public boolean processTask(@NotNull ScannerInitializer scannerInitializer, @NotNull Consumer<ScannerInitializer> completionCallback) {
        String inventoryItem = scannerInitializer.getInventoryItem().getInventoryName();
        // Get the configuration containing the uploaded file
        Configuration configuration = Objects.requireNonNull(scannerInitializer.getScannerConfiguration()
                .stream()
                .filter(c -> c.getName().equals(CONFIG_KEY_FILENAME))
                .findFirst().orElse(null));

        byte[] contentInByte = configuration.getUpload();
        log.debug("contentInByte: {}", contentInByte.length);


        try {
            readExcelRows(scannerInitializer, contentInByte );
            log.debug("Processing Flexera Report inventory item: {}", inventoryItem);
            return true;
        }catch (Exception e){
            log.error("Error when connecting to backend: {}", e.getMessage());
            scannerInitializerService.updateScannerFeedback("Error when connecting to backend: "+ e.getMessage(), scannerInitializer);
            return false;
        }

    }

    private void readExcelRows( ScannerInitializer scannerInitializer, byte[] contentInByte) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contentInByte)){
            OPCPackage pkg = OPCPackage.open(byteArrayInputStream);
            XSSFWorkbook workbook = new XSSFWorkbook(pkg);
            XSSFSheet sheet = workbook.getSheetAt(0);

        Map<Integer, String> columnIndexNamesMap = createColumnNamesMap(sheet, 0);
        Iterator<Row> rowIterator = sheet.iterator();
        //skip first line with columnNames
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {

            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            Map<String, Object> rowData = new LinkedHashMap<>();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String columnName= columnIndexNamesMap.get(cell.getColumnIndex());
                if ( columnName!= null) {
                    switch (cell.getCellType()) {
                        case STRING -> rowData.put(columnName, cell.getStringCellValue());
                        case NUMERIC -> rowData.put(columnName, cell.getNumericCellValue());
                        case BOOLEAN -> rowData.put(columnName, cell.getBooleanCellValue());
                        case FORMULA -> rowData.put(columnName, cell.getCellFormula());
                    }

                }
            }
            if(!rowData.isEmpty())sendIntoStream(scannerInitializer.getId(), rowData);

        }
            workbook.close();
            pkg.close();

        } catch (IOException | OpenXML4JException e) {
            log.error("Exception when processing errorMessage", e);
            scannerInitializerFactory.saveWithFeedBack(scannerInitializer, List.of("Error when processing file: " + e.getMessage()), ScannerInitializerStatus.STOPPED);
        }
    }

    /**find the column names in the first row
     *
     * @param sheet
     * @return
     */
    private Map<Integer, String> createColumnNamesMap(Sheet sheet, int rowWithCaptions) {


        Map<Integer, String> columnIndexNameMap = new HashMap<>();
        int columnIndex;

        Row row1 = sheet.getRow(rowWithCaptions);
        for (int i = 0; i < row1.getLastCellNum() + 1; i++) {
            columnIndex = i;
            Cell cell = row1.getCell(columnIndex);
            if(cell!=null) {
                String columnName = (cell.getCellType() == CellType.NUMERIC) ? Long.toString((long)Math.floor(cell.getNumericCellValue())) : cell.getStringCellValue();
                if (columnName != null)
                    columnIndexNameMap.put(columnIndex, columnName.trim());
            }
        }
        return columnIndexNameMap;
    }


    private void sendIntoStream(UUID scannerInitId, Map<String, Object> rowData) {

        FossReportServiceWorkData fossReportServiceWorkData = new FossReportServiceWorkData(scannerInitId, rowData);
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("status_request", "question", actualTimestamp, fossReportServiceWorkData);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String message = objectMapper.writeValueAsString(workTask);
            natsService.sendWorkMessageToStream("work.fossreport", message.getBytes(StandardCharsets.UTF_8));
            log.debug("sending message to foss service: {}", message);
        } catch (Exception e) {
            log.debug("Error with foss service connection: " + e.getMessage());
        }
    }


    @Override
    public List<String> getSupportedConfigurationKeys() {
        return List.of(CONFIG_KEY_FILENAME, CONFIG_KEY_USE_LICENSE_MATCHER, CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER);
    }

    @Override
    public List<String> getRequiredConfigurationKeys() {
        return List.of(CONFIG_KEY_FILENAME);
    }

    @Override
    public Configuration.Type getTypeOfConfiguration(String key) {
        log.debug("getTypeOfConfiguration called for key: {}", key);
        switch (key) {
            case CONFIG_KEY_FILENAME:
                return Configuration.Type.FILE_UPLOAD;
            case CONFIG_KEY_USE_LICENSE_MATCHER:
            case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER:
                return Configuration.Type.BOOLEAN;

        }
        return super.getTypeOfConfiguration(key);
    }

    @Override
    public String getDefaultConfigurationValue(String k, InventoryItem inventoryItem) {
        switch(k) {
            case CONFIG_KEY_USE_LICENSE_MATCHER: return ""+DEFAULT_USE_LICENSE_MATCHER;
            case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER: return ""+DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
        }
        return super.getDefaultConfigurationValue(k, inventoryItem);
    }
}
