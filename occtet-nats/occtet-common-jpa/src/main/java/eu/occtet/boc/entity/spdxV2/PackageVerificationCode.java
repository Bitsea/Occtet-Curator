/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.entity.spdxV2;

import jakarta.persistence.*;
import java.util.List;

@Embeddable
public class PackageVerificationCode {

    @Column(nullable = false)
    private String packageVerificationCodeValue;

    @ElementCollection
    @CollectionTable(name = "package_verification_excluded_files", joinColumns = @JoinColumn(name = "package_id"))
    @Column(name = "excluded_file_name")
    private List<String> packageVerificationCodeExcludedFiles;

    public String getPackageVerificationCodeValue() {
        return packageVerificationCodeValue;
    }

    public List<String> getPackageVerificationCodeExcludedFiles() {
        return packageVerificationCodeExcludedFiles;
    }

    public void setPackageVerificationCodeValue(String packageVerificationCodeValue) {
        this.packageVerificationCodeValue = packageVerificationCodeValue;
    }

    public void setPackageVerificationCodeExcludedFiles(List<String> packageVerificationCodeExcludedFiles) {
        this.packageVerificationCodeExcludedFiles = packageVerificationCodeExcludedFiles;
    }
}
