package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Copyright;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;


public interface CopyrightRepository extends JmixDataRepository<Copyright, UUID> {

   List<Copyright> findAll();
   List<Copyright> findByCopyrightText(String copyrightText);
   List<Copyright> findCopyrightsByCurated(Boolean curated);
   List<Copyright> findCopyrightsByGarbage(Boolean garbage);
   List<Copyright> findCopyrightsByCodeLocation(CodeLocation codeLocation);
}