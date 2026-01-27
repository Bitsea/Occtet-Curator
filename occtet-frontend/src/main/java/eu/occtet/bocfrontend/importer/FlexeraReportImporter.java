package eu.occtet.bocfrontend.importer;

import eu.occtet.boc.model.FossReportServiceWorkData;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.TaskStatus;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.service.NatsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class FlexeraReportImporter extends Importer {

    private static final Logger log = LogManager.getLogger(FlexeraReportImporter.class);


    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private  NatsService natsService;


    protected FlexeraReportImporter() {
        super("Flexera_Report_Import");
    }


    private static final String CONFIG_KEY_USE_LICENSE_MATCHER = "UseLicenseMatcher";
    private static final String CONFIG_KEY_FILENAME= "fileName";
    private static final String CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER = "UseFalseCopyrightFilter";
    private static final boolean DEFAULT_USE_LICENSE_MATCHER = true;
    private static final boolean DEFAULT_USE_FALSE_COPYRIGHT_FILTER = true;


    @Override
    public boolean prepareAndStartTask(CuratorTask curatorTask) {
        // Get the configuration containing the uploaded file
        Configuration configuration = Objects.requireNonNull(curatorTask.getTaskConfiguration()
                .stream()
                .filter(c -> c.getName().equals(CONFIG_KEY_FILENAME))
                .findFirst().orElse(null));

        byte[] contentInByte = configuration.getUpload();
        log.debug("contentInByte: {}", contentInByte.length);

        log.info("Processing Flexera Report for Project: {}...", curatorTask.getProject().getProjectName());
        return readExcelRows(curatorTask, contentInByte );

    }

    private boolean readExcelRows(CuratorTask curatorTask, byte[] contentInByte) {
        boolean res=false;
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
                if(!rowData.isEmpty()){
                    res = startTask(curatorTask, rowData);
                }

            }
            workbook.close();
            pkg.close();

        } catch (Exception e) {
            log.error("Exception when processing errorMessage", e);
            curatorTaskService.saveWithFeedBack(curatorTask, List.of("Error when processing file: " + e.getMessage()), TaskStatus.CANCELLED);
            res=false;
        }
        return res;
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


    private boolean startTask(CuratorTask task, Map<String, Object> rowData)  {

        FossReportServiceWorkData fossReportServiceWorkData = new FossReportServiceWorkData(task.getId(), rowData);

        return curatorTaskService.saveAndRunTask(task,fossReportServiceWorkData,"converted flexera row to work task");

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
        return switch (key) {
            case CONFIG_KEY_FILENAME -> Configuration.Type.FILE_UPLOAD;
            case CONFIG_KEY_USE_LICENSE_MATCHER, CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER -> Configuration.Type.BOOLEAN;
            default -> super.getTypeOfConfiguration(key);
        };
    }

    @Override
    public String getDefaultConfigurationValue(String k) {
        return switch (k) {
            case CONFIG_KEY_USE_LICENSE_MATCHER -> "" + DEFAULT_USE_LICENSE_MATCHER;
            case CONFIG_KEY_USE_FALSE_COPYRIGHT_FILTER -> "" + DEFAULT_USE_FALSE_COPYRIGHT_FILTER;
            default -> super.getDefaultConfigurationValue(k);
        };
    }
}
