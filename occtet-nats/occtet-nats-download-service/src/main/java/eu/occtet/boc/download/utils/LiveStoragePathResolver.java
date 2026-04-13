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

package eu.occtet.boc.download.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Profile("live")
public class LiveStoragePathResolver implements StoragePathResolver {

    private final Path containerStorageRoot;
    private final String rootFolderName;

    public LiveStoragePathResolver(@Value("${occtet.storage.root:/project_data}") String storageRootPath) {
        this.containerStorageRoot = Paths.get(storageRootPath).toAbsolutePath().normalize();

        this.rootFolderName = this.containerStorageRoot.getFileName() != null ? this.containerStorageRoot.getFileName().toString() : "";
    }

    @Override
    public Path resolveSystemPath(String uiSystemPath) {
        if (uiSystemPath == null || uiSystemPath.isBlank()) {
            return containerStorageRoot;
        }

        String cleanPath = uiSystemPath.replace('\\', '/');

        cleanPath = cleanPath.replaceFirst("^/+", "");

        // Case: double-prefixing dynamically based on the configured root
        if (!rootFolderName.isEmpty()) {
            if (cleanPath.startsWith(rootFolderName + "/")) {
                cleanPath = cleanPath.substring(rootFolderName.length() + 1);
            } else if (cleanPath.equals(rootFolderName)) {
                cleanPath = "";
            }
        }

        return containerStorageRoot.resolve(cleanPath).normalize();
    }
}