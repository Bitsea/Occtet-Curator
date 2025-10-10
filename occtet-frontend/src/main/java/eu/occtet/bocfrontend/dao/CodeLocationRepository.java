package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.CodeLocation;
import io.jmix.core.repository.JmixDataRepository;

import java.util.UUID;

public interface CodeLocationRepository  extends JmixDataRepository<CodeLocation, UUID> {
}
