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

@Entity
@Table(name = "CREATION_INFO_ENTITY")
public class CreationInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "creation_info_gen")
    @SequenceGenerator(name = "creation_info_gen", sequenceName = "creation_info_entity_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String created;

    @Column(name = "creators", nullable = false)
    private String creators;

    @Lob
    private String comment;

    @Column(name = "license_list_version")
    private String licenseListVersion;

    public String getComment() {
        return comment;
    }

    public Long getId() {
        return id;
    }

    public String getCreated() {
        return created;
    }

    public String getCreators() {
        return creators;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public void setCreators(String creators) {
        this.creators = creators;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getLicenseListVersion() {
        return licenseListVersion;
    }

    public void setLicenseListVersion(String licenseListVersion) {
        this.licenseListVersion = licenseListVersion;
    }
}