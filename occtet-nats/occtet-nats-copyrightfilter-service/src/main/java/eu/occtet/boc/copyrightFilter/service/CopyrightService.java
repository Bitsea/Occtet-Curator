package eu.occtet.boc.copyrightFilter.service;


import eu.occtet.boc.copyrightFilter.factory.CopyrightFactory;
import eu.occtet.boc.entity.Copyright;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CopyrightService {

    private static final Logger log = LogManager.getLogger(CopyrightService.class);



    @Autowired
    private CopyrightFactory copyrightFactory;



    public void updateCopyrightAsGarbage(Copyright c) {
        copyrightFactory.updateCopyrightAsGarbage(c);
    }
}
