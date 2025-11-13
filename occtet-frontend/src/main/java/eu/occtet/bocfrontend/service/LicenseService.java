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
import eu.occtet.bocfrontend.factory.LicenseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LicenseService {

    private static final Logger log = LogManager.getLogger(LicenseService.class);
    private final LicenseRepository licenseRepository;

    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private LicenseFactory licenseFactory;

    public LicenseService(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    public List<License> findLicensesByProject(Project project){
        List<SoftwareComponent> softwareComponents = softwareComponentService.findSoftwareComponentsByProject(project);
        List<License> licenses = new ArrayList<>();
        softwareComponents.forEach(sc->licenses.addAll(sc.getLicenses()));
        return licenses;
    }

    public List<License> findLicenseByPriority(Integer priority){
        return licenseRepository.findLicensesByPriority(priority);
    }

    public List<License> findLicenseByCurated(Boolean isCurated){
        return licenseRepository.findLicensesByCurated(isCurated);
    }

    public License createLicense(Integer priority, String licenseType,String licenseText, String licenseName,
                                 String detailsUrl, boolean isModified, boolean curated, boolean isSpdx){

        return licenseFactory.create(priority,licenseType,licenseText,licenseName,detailsUrl,isModified,curated,isSpdx);
    }

}
