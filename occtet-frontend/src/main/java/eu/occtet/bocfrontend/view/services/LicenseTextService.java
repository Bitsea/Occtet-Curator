package eu.occtet.bocfrontend.view.services;

import eu.occtet.bocfrontend.dao.LicenseDao;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.entity.License;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LicenseTextService {

    @Autowired
    private LicenseDao licenseDao;

    @Autowired
    private LicenseRepository licenseRepository;

    public List<Pair<License,Float>> findBySimilarity(String text, int limit) {
        List<Pair<Long, Float>> ids = licenseDao.findLicenseIdsSimilarTo(text, limit);
        return ids.stream().map(
                LongFloatPair ->{
                    License lt= licenseRepository.findById(LongFloatPair.getKey()).get();
                    return Pair.of(lt,LongFloatPair.getValue());
                })
                .collect(Collectors.toList());
    }
}
