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

package eu.occtet.boc.download.service;

import com.github.packageurl.PackageURL;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public interface DownloadStrategy {

    default boolean canHandle(URL durl, String version) { return false; }
    default boolean canHandle(PackageURL purl) { return false; }
    default boolean canHandle(String pckName, String version) { return false; }

    default Path download(URL durl, Path targetDirectory, boolean isMainPkg) throws IOException {
        throw new UnsupportedOperationException("URL download not supported by this strategy");
    }

    default Path download(PackageURL purl, Path targetDirectory, boolean isMainPkg) throws IOException {
        throw new UnsupportedOperationException("PURL download not supported by this strategy");
    }

    default Path download(String name, String version, Path targetDirectory, boolean isMainPkg) throws IOException {
        throw new UnsupportedOperationException("Name/Version download not supported by this strategy");
    }
}
