package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SoftwareComponentService {

    private static final Logger log = LogManager.getLogger(SoftwareComponentService.class);

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Lazy
    @Autowired
    private InventoryItemService inventoryItemService;

    public List<SoftwareComponent> findSoftwareComponentsByProject(Project project){
        List<InventoryItem> inventoryItems = inventoryItemService.findInventoryItemsOfProject(project);
        List<SoftwareComponent> listingSoftwareComponent = new ArrayList<>();
        inventoryItems.forEach(i-> {
            if (i.getSoftwareComponent() != null) {
                listingSoftwareComponent.add(i.getSoftwareComponent());
            }
        });
        return listingSoftwareComponent;
    }

    public List<SoftwareComponent> findSoftwareComponentsByLicense(License license){
        List<SoftwareComponent> listingSoftwareComponent = softwareComponentRepository.findAll();
        listingSoftwareComponent.removeIf(sc->sc.getLicenses().stream().noneMatch(l->l.equals(license)));
        return listingSoftwareComponent;
    }

    public List<SoftwareComponent> findSoftwareComponentsByCurated(Boolean isCurated){
        return softwareComponentRepository.findSoftwareComponentsByCurated(isCurated);
    }

    public Set<String> getCVEDescriptionsList(String cve){
        if (cve == null || cve.isBlank()) return new HashSet<>();

        String[] split = cve.split("\\s+");
        Set<String> result = new HashSet<>();
        for (String s : split) {
            if (s.startsWith("CVE-")) result.add(s);
        }
        return result;
    }

    public Set<String> getAllCVEFoundInSoftwareComponents(List<SoftwareComponent> softwareComponents){
        //TODO check ticket 978
        return new HashSet<>();
    }

    public List<SoftwareComponent> findSoftwareComponentsByIsVulnerable(Boolean IsVulnerable){
        //TODO check ticket 978
        return new ArrayList<>();
    }
}
