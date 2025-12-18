package eu.occtet.boc.ai.copyrightFilter.dao;

import eu.occtet.boc.entity.Copyright;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CopyrightRepository extends JpaRepository<Copyright, Long> {

    List<Copyright> findByCopyrightText(String copyrightText);


}
