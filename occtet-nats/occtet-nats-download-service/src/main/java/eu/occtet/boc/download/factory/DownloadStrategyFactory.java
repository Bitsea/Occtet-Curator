/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.download.factory;

import com.github.packageurl.PackageURL;
import eu.occtet.boc.download.strategies.DownloadStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Factory responsible for selecting the appropriate {@link DownloadStrategy}
 */
@Component
public class DownloadStrategyFactory {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private List<DownloadStrategy> strategies;

    public Optional<DownloadStrategy> findForUrl(URL durl, String version) {
        return strategies.stream().filter(s -> s.canHandle(durl, version)).findFirst();
    }

    public Optional<DownloadStrategy> findForPurl(PackageURL purl) {
        return strategies.stream().filter(s -> s.canHandle(purl)).findFirst();
    }

    public Optional<DownloadStrategy> findForName(String name, String version) {
        return strategies.stream().filter(s -> s.canHandle(name, version)).findFirst();
    }
}
