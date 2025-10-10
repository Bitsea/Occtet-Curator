package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ExtendWith(AuthenticatedAsAdmin.class)
public class LicenseServiceTest {

    @Autowired
    private LicenseService licenseService;
    @Autowired
    private DataManager dataManager;

    @Test
    void testGetLicenseLocatedAtProject(){
        Project project = dataManager.create(Project.class);
        License license1 = dataManager.create(License.class);
        License license2 = dataManager.create(License.class);
        SoftwareComponent softwareComponent = dataManager.create(SoftwareComponent.class);
        InventoryItem inventoryItem = dataManager.create(InventoryItem.class);

        project.setProjectName("LicenseServiceTestProject");
        inventoryItem.setProject(project);
        softwareComponent.setName("softwareComponent1");
        softwareComponent.setVersion("1.0");
        softwareComponent.setLicenses(new java.util.ArrayList<>());
        softwareComponent.getLicenses().add(license1);
        softwareComponent.getLicenses().add(license2);
        inventoryItem.setSoftwareComponent(softwareComponent);

        dataManager.save(project);
        dataManager.save(license1);
        dataManager.save(license2);
        dataManager.save(softwareComponent);
        dataManager.save(inventoryItem);

        assertEquals(2, licenseService.findLicensesByProject(project).size());
    }
}
