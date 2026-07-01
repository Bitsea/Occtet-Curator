/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.service;


import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.SoftwareComponentLicenseUsage;
import eu.occtet.bocfrontend.factory.UsageLicenseFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LicenseService {

    private static final Logger log = LogManager.getLogger(LicenseService.class);

    @Autowired
    private SoftwareComponentService softwareComponentService;
    @PersistenceContext
    private EntityManager entityManager;

    public List<SoftwareComponentLicenseUsage> findUsageLicensesByProject(Project project){
        List<SoftwareComponent> softwareComponents = softwareComponentService.findSoftwareComponentsByProject(project);
        List<SoftwareComponentLicenseUsage> licenses = new ArrayList<>();
        softwareComponents.forEach(sc->licenses.addAll(sc.getUsageLicenses()));
        return licenses;
    }


    /**
     * removes license and associated softwarecomponentusage from the db the hard way
     * other way around this did not work out
     * @param licenseIds
     */
    @Transactional
    public void removeLicensesHard(Set<Long> licenseIds) {
        if (licenseIds == null || licenseIds.isEmpty()) {
            return;
        }

        entityManager.createQuery("DELETE FROM SoftwareComponentLicenseUsage u WHERE u.template.id IN :ids")
                .setParameter("ids", licenseIds)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM License l WHERE l.id IN :ids")
                .setParameter("ids", licenseIds)
                .executeUpdate();
    }



}
