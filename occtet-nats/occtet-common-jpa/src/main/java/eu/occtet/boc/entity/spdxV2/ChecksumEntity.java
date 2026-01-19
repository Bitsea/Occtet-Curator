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


@Entity
@Table(name = "CHECKSUM_ENTITY")
public class ChecksumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "package_id")
    private SpdxPackageEntity pkg;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private SpdxFileEntity spdxFileEntity;

    @Column(nullable = false)
    private String algorithm;

    @Column(nullable = false, name = "checksum_value")
    private String checksumValue;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public SpdxPackageEntity getPkg() {
        return pkg;
    }

    public SpdxFileEntity getSpdxFile() {
        return spdxFileEntity;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getChecksumValue() {
        return checksumValue;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setChecksumValue(String checksumValue) {
        this.checksumValue = checksumValue;
    }

    public void setPkg(SpdxPackageEntity pkg) {
        this.pkg = pkg;
    }

    public void setSpdxFile(SpdxFileEntity spdxFileEntity) {
        this.spdxFileEntity = spdxFileEntity;
    }
}
