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

package eu.occtet.bocfrontend.scanner;


import eu.occtet.bocfrontend.entity.ScannerInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * scanner which is so dumb that it finds nothing.
 * This is a proof of concept class.
 */
@Service
public class DumbScanner extends Scanner {
    public DumbScanner() {
        super("Dumb");
    }

    private static final Logger log = LogManager.getLogger(DumbScanner.class);


    @Override
    public boolean processTask(@Nonnull ScannerInitializer scannerInitializer,
                               @Nonnull Consumer<ScannerInitializer> completionConsumer) {
        // find nothing, don't call findingConsumer, just finish.
        log.debug("DumbScanner: doing nothing for project {}", scannerInitializer.getProject().getProjectName());
        completionConsumer.accept(scannerInitializer);
        return true;
    }

}
