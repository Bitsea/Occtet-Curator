package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@Transactional
public class CopyrightServiceTest {

    private static final Logger log = LogManager.getLogger(CopyrightServiceTest.class);

    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private CopyrightService service;

    private final static Path BASEPATH_TEST = Paths.get("src", "test", "resources/garbage-copyrights-test/garbage-copyrights-test.yml");

    @Test
    public void readYML_Test(){

        List<String> listFile = service.readYML(new File(BASEPATH_TEST.toFile().getAbsolutePath()));
        assertFalse(listFile.isEmpty());
        assertEquals(3,listFile.size());

        for(String copyright : listFile){

            log.info("Copyright: "+copyright);
        }
    }
}
