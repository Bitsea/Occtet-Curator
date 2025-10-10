package eu.occtet.boc.copyrightFilter.dao;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CopyrightRepository extends JpaRepository<Copyright, Long> {
    List<Copyright> findByCopyrightTextAndCodeLocation(String copyrightText, CodeLocation codeLocation);
    List<Copyright> findByCopyrightText(String copyrightText);
}
