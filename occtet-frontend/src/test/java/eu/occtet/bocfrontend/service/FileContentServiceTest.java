package eu.occtet.bocfrontend.service;


import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.factory.CodeLocationFactory;
import eu.occtet.bocfrontend.model.FileResult;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(AuthenticatedAsAdmin.class)
@SpringBootTest
public class FileContentServiceTest {

    private static final Logger log = LogManager.getLogger(FileContentServiceTest.class);

    @Autowired
    private FileContentService fileContentService;
    @Autowired
    private CodeLocationFactory codeLocationFactory;

    @Test
    void getFileContent_fromRelativePath_succeeds() {
        InventoryItem rootItem = mock(InventoryItem.class);

        String projectRootPath = Paths.get("").toAbsolutePath().toString();
        when(rootItem.getBasePath()).thenReturn(projectRootPath);
        when(rootItem.getParent()).thenReturn(null);

        CodeLocation codeLocation = codeLocationFactory.create(null,
                "src/test/resources/FileContentTestFile", 0, 0);

        FileResult result = fileContentService.getFileContent(codeLocation, rootItem);

        assertInstanceOf(FileResult.Success.class, result);
        FileResult.Success successResult = (FileResult.Success) result;

        String expectedContent = "Hello World!";

        assertEquals(expectedContent, successResult.content());
    }
}
