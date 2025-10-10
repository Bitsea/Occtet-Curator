package eu.occtet.boc.fossreport.factory;

import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.fossreport.dao.CopyrightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyrightFactory {

    private static final Logger log = LoggerFactory.getLogger(CopyrightFactory.class);

    @Autowired
    private CopyrightRepository copyrightRepository;

    public Copyright create(String copyrightString, CodeLocation codeLocation){
        log.debug("Creating Copyright with copyright text: {} and code location: {}", copyrightString, codeLocation);
        return copyrightRepository.save(new Copyright(copyrightString, codeLocation));
    }


}
