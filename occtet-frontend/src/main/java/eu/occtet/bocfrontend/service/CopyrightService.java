package eu.occtet.bocfrontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.*;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jmix.core.FileRef;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.upload.TemporaryStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class CopyrightService {


    private static final Logger log = LogManager.getLogger(CopyrightService.class);
    private final CopyrightRepository copyrightRepository;

    @Autowired
    private InventoryItemService inventoryItemService;

    private static final Path BASEPATH_YML = Paths.get("src", "main", "resources/garbage-Copyrights/garbage-copyrights.yml");
    private static final Path BASEPATH_JSON = Paths.get("src","main","resources/garbage-Copyrights/garbage-copyrights.json");
    private static final String FILENAME_YML = "garbage-copyrights.yml";

    @Autowired
    private TemporaryStorage temporaryStorage;

    public CopyrightService(CopyrightRepository copyrightRepository) {
        this.copyrightRepository = copyrightRepository;
    }

    public List<Copyright> findCopyrightsByProject(Project project){
        List<InventoryItem> inventoryItems = inventoryItemService.findInventoryItemsOfProject(project);
        List<Copyright> copyrights = new ArrayList<>();
        inventoryItems.forEach(i->copyrights.addAll(i.getCopyrights()));
        return copyrights;
    }

    public List<Copyright> findCopyrightsBySoftwareComponent(SoftwareComponent softwareComponent){
        List<InventoryItem> inventoryItems = inventoryItemService.findInventoryItemsOfSoftwareComponent(softwareComponent);
        List<Copyright> copyrights = new ArrayList<>();
        inventoryItems.forEach(i->copyrights.addAll(i.getCopyrights()));
        return copyrights;
    }

    public List<Copyright> findCopyrightsByGarbage(Boolean isGarbage){
        return copyrightRepository.findCopyrightsByGarbage(isGarbage);
    }

    public List<Copyright> findCopyrightsByCurated(Boolean isCurated){
        return copyrightRepository.findCopyrightsByCurated(isCurated);
    }

    public void createYML(List<Copyright> copyrightList) {

        try {
            Map<String, List<String>> data = new HashMap<>();
            data.put("Copyrights", copyrightList.stream().map(Copyright::getCopyrightText).toList());
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writeValue(new File(BASEPATH_YML.toFile().getAbsolutePath()), data);
        } catch (IOException e) {
        }
    }

    public FileRef getYmlFileRef() {

        try {
            FileInputStream file = new FileInputStream(BASEPATH_YML.toFile().getAbsolutePath());
            UUID id = temporaryStorage.saveFile(file.readAllBytes());
            FileRef ref = temporaryStorage.putFileIntoStorage(id, FILENAME_YML);
            return ref;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> readYML(File file) {

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, List<String>> dataYML = mapper.readValue(file, Map.class);
            String key = dataYML.keySet().stream().toList().get(0);
            return dataYML.get(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setGarbageCopyrightsInJSON(List<String> garbageCopyrights) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            File jsonFile = new File(BASEPATH_JSON.toFile().getAbsolutePath());
            Map<String,List<String>> garbage;

            if (jsonFile.exists() && jsonFile.length() > 0) {
                garbage = mapper.readValue(jsonFile,  new TypeReference<>() {});
            } else {
                garbage  = new HashMap<>();
            }
            List<String> garbageListe = garbage.computeIfAbsent("Copyright", k -> new ArrayList<>());

            if (!(garbageListe instanceof ArrayList)) {
                garbageListe = new ArrayList<>();
                garbage.put("Copyright", garbageListe);
            }
            for(String copyright : garbageCopyrights){
                if(!garbageListe.contains(copyright)){
                    garbageListe.add(copyright);
                }
            }
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(jsonFile, garbage);

        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public File createFileUploadCopyrights(FileUploadSucceededEvent<FileUploadField> event){

        try{
            String fileName = event.getFileName();
            byte[] fileContent = event.getSource().getValue();
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);

            if(fileContent != null) {
                fos.write(fileContent);
            }
            return file;

        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

}


