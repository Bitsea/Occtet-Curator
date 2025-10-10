package eu.occtet.boc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.UsageType;
import org.junit.Assert;
import org.junit.Test;

public class MicroserviceDescriptorTest {

    @Test
    public void testMicroserviceDescriptor() throws JsonProcessingException {
        MicroserviceDescriptor md = new MicroserviceDescriptor("name","description","version",
                "someWorkData", UsageType.RUNNABLE_BY_SERVICE);
        String result = (new ObjectMapper()).writerFor(MicroserviceDescriptor.class).writeValueAsString(md);
        Assert.assertEquals("{\"type\":\"descriptor\",\"name\":\"name\",\"description\":\"description\",\"version\":\"version\",\"acceptableWorkData\":\"someWorkData\",\"usageType\":\"RUNNABLE_BY_SERVICE\"}",result);
    }

}
