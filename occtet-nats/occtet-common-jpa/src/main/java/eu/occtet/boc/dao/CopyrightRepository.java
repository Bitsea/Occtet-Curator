package eu.occtet.boc.dao;

import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CopyrightRepository extends JpaRepository<Copyright, Long> {

    List<Copyright> findByCopyrightText(String copyrightText);

    List<Copyright> findByFilesIn(List<File> files);

    List<Copyright> findByCopyrightTextIn(Collection<String> copyrightTexts);

    List<Copyright> findByGarbageTrue();
}
