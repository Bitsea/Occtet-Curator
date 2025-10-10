package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class UtilitiesTest {

    @Test
    void test_handleCasing(){
        String example = "FileNameForFlexeraReportXy";
        assertEquals(
                "File Name For Flexera Report Xy",
                new Utilities().handleCasing(example)
        );
    }
}
