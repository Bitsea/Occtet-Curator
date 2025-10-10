package eu.occtet.boc.spdx.factory;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.spdx.dao.CopyrightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyrightFactory {

    @Autowired
    private CopyrightRepository copyrightRepository;

    public Copyright create(String copyrightString, CodeLocation codeLocation){
        Copyright copyright = new Copyright(copyrightString, codeLocation);

        return copyrightRepository.save(copyright);
    }
}
