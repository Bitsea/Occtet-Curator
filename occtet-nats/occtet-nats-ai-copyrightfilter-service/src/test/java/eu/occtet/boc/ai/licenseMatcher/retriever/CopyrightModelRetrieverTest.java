package eu.occtet.boc.ai.licenseMatcher.retriever;

import eu.occtet.boc.ai.copyrightFilter.retriever.CopyrightRetriever;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@SpringBootTest
//@AutoConfigureDataJpa
// NOTICE: If you comment out the next two lines, the test will use the DB connection configured in test/resources/application.properties
//@AutoConfigureTestEntityManager
//@AutoConfigureTestDatabase
public class CopyrightModelRetrieverTest {
    private static final Logger log = LogManager.getLogger(CopyrightModelRetrieverTest.class);

    @Autowired
    CopyrightRetriever copyrightRetriever;

    //@Test
    public void similaritySearchTest() {
        copyrightRetriever.loadVectorStore("bad copyright examples");
        List<Document> docs= copyrightRetriever.similaritySearch( 60, 0.5, "Get copyright examples", "fileName == 'bad-copyrights.txt'");
        for(Document d: docs){
            log.debug("META {}", d.getMetadata());
            log.debug("CONTENT {}", d.getFormattedContent());
        }
        //Here the output depends on how many files you already uploaded into the vector store
        assertEquals(2,docs.size());
    }

}