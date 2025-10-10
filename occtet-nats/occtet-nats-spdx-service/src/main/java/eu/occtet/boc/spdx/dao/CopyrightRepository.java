package eu.occtet.boc.spdx.dao;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopyrightRepository extends JpaRepository<Copyright, Long> {
    Optional<Copyright> findByCopyrightTextAndCodeLocation(String copyrightText, CodeLocation codeLocation);
    Optional<Copyright> findByCopyrightText(String copyrightText);
    List<Copyright> findByCodeLocation(CodeLocation codeLocation);
}
