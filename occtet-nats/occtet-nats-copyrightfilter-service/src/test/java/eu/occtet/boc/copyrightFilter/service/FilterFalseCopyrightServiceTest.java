package eu.occtet.boc.copyrightFilter.service;

import eu.occtet.boc.copyrightFilter.preprocessor.CopyrightPreprocessor;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@SpringBootTest
//@AutoConfigureDataJpa
public class FilterFalseCopyrightServiceTest {


    //@Test
    void filterFalsCopyrightsWithGarbageFile() {
        CopyrightFilterService filterFalseCopyrightService= new CopyrightFilterService();
        InventoryItem item = new InventoryItem();
        Copyright copyright1 = new Copyright();
        copyright1.setCopyrightText("valid copyright");
        Copyright copyright2 = new Copyright();
        copyright2.setCopyrightText("Copyright (C) bve[<author>]");
        item.setCopyrights(List.of(copyright1, copyright2));

        List<String> copyrightTexts = new ArrayList<>(List.of("valid copyright", "Copyright (C) bve[<author>]"));

        List<String> result = filterFalseCopyrightService.filterFalsCopyrightsWithGarbageFile(copyrightTexts, item);

        assertEquals(List.of("valid copyright"), result);
    }


}
