package eu.occtet.boc.spdx.service;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.spdx.dao.CopyrightRepository;
import eu.occtet.boc.spdx.factory.CopyrightFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CopyrightService {

    private static final Logger log = LogManager.getLogger(CopyrightService.class);

    @Autowired
    private CopyrightFactory copyrightFactory;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private CodeLocationService codeLocationService;

    public Copyright findOrCreateCopyright(String copyrightString, CodeLocation codeLocation){
        Optional<Copyright> copyright = copyrightRepository.findByCopyrightTextAndCodeLocation(copyrightString,
                codeLocation);
        if (!copyright.isPresent()) {
            copyright = Optional.ofNullable(copyrightFactory.create(copyrightString, codeLocation));
        }

        return copyright.get();
    }


}
