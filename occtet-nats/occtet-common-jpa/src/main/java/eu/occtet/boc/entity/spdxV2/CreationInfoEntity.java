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
public class CreationInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String created;

    @ElementCollection
    @CollectionTable(name = "creator", joinColumns = @JoinColumn(name = "creation_info_id"))
    @Column(name = "creator_name", nullable = false)
    private List<String> creators;

    @Lob
    private String comment;

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

    public List<String> getCreators() {
        return creators;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public void setCreators(List<String> creators) {
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