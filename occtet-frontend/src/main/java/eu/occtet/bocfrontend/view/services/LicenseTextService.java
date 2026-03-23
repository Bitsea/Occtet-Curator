package eu.occtet.bocfrontend.view.services;

import eu.occtet.bocfrontend.dao.LicenseDao;
import eu.occtet.bocfrontend.dao.TemplateLicenseRepository;
import eu.occtet.bocfrontend.entity.TemplateLicense;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LicenseTextService {

    @Autowired
    private LicenseDao licenseDao;

    @Autowired
    private TemplateLicenseRepository templateLicenseRepository;

    public List<Pair<TemplateLicense, Float>> findBySimilarity(String text, int limit) {
        List<Pair<Long, Float>> ids = licenseDao.findLicenseIdsSimilarTo(text, limit);

        return ids.stream()
                .map(idFloatPair -> {
                    TemplateLicense template = templateLicenseRepository.findById(idFloatPair.getKey()).orElse(null);

                    if (template == null) {
                        return null;
                    }

                    return Pair.of(template, idFloatPair.getValue());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
