package eu.occtet.boc.fossreport.service;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.fossreport.dao.CopyrightRepository;
import eu.occtet.boc.fossreport.factory.CopyrightFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CopyrightService {

    private static final Logger log = LoggerFactory.getLogger(CopyrightService.class);



    @Autowired
    private CopyrightFactory copyrightFactory;
    @Autowired
    private CopyrightRepository copyrightRepository;


    public Copyright findOrCreateCopyright(String copyrightString, CodeLocation codeLocation) {
        List<Copyright> copyright = copyrightRepository.findByCopyrightTextAndCodeLocation(copyrightString,
                codeLocation);
        if (!copyright.isEmpty() && copyright.getFirst() != null) {
            copyright.getFirst();
        }

        return copyrightFactory.create(copyrightString, codeLocation);
    }


}
