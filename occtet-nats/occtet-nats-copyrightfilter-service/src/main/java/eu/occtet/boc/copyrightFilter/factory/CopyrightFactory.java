package eu.occtet.boc.copyrightFilter.factory;


import eu.occtet.boc.copyrightFilter.dao.CopyrightRepository;
import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyrightFactory {

    @Autowired
    private CopyrightRepository copyrightRepository;


    public void updateCopyrightAsGarbage(Copyright c) {
        c.setGarbage(true);
        copyrightRepository.save(c);
    }
}
