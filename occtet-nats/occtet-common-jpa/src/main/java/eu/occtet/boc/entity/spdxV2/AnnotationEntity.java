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
@Table(name ="ANNOTATION_ENTITY")
public class AnnotationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, name= "spdx_element_id")
    private String spdxElementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spdx_package_id")
    private SpdxPackageEntity pkg;

    @Column(nullable = false, name = "annotation_date")
    private String annotationDate;

    @Column(nullable = false, name = "annotation_type")
    private String annotationType;

    @Column(nullable = false)
    private String annotator;

    @Lob
    @Column(nullable = false)
    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpdxElementId() {
        return spdxElementId;
    }

    public void setSpdxElementId(String spdxElementId) {
        this.spdxElementId = spdxElementId;
    }

    public String getAnnotationDate() {
        return annotationDate;
    }

    public void setAnnotationDate(String annotationDate) {
        this.annotationDate = annotationDate;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }

    public String getAnnotator() {
        return annotator;
    }

    public void setAnnotator(String annotator) {
        this.annotator = annotator;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public SpdxPackageEntity getPkg() {
        return pkg;
    }

    public void setPkg(SpdxPackageEntity pkg) {
        this.pkg = pkg;
    }
}
