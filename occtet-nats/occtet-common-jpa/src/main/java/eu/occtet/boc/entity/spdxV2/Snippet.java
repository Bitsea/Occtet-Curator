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
public class Snippet {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String spdxId;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id", nullable = false)
    private SpdxDocumentRoot spdxDocument;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, name= "snippet_from_file", length = 2048)
    private String snippetFromFile;

    @ElementCollection
    @CollectionTable(name = "snippet_ranges", joinColumns = @JoinColumn(name = "snippet_id"))
    private List<Range> ranges;

    @Column(name= "license_concluded",length = Integer.MAX_VALUE)
    private String licenseConcluded;

    @ElementCollection
    @CollectionTable(name = "snippet_licenses", joinColumns = @JoinColumn(name = "snippet_id"))
    private List<String> licenseInfoInSnippets;

    @Column(name= "copyright_text",length = Integer.MAX_VALUE)
    private String copyrightText;

    public SpdxDocumentRoot getSpdxDocument() {
        return spdxDocument;
    }

    public String getSPDXID() {
        return spdxId;
    }

    public String getLicenseConcluded() {
        return licenseConcluded;
    }

    public String getName() {
        return name;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public List<String> getLicenseInfoInSnippets() {
        return licenseInfoInSnippets;
    }

    public String getSnippetFromFile() {
        return snippetFromFile;
    }

    public void setSpdxDocument(SpdxDocumentRoot spdxDocument) {
        this.spdxDocument = spdxDocument;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public void setLicenseConcluded(String licenseConcluded) {
        this.licenseConcluded = licenseConcluded;
    }

    public void setSPDXID(String SPDXID) {
        this.spdxId = SPDXID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLicenseInfoInSnippets(List<String> licenseInfoInSnippets) {
        this.licenseInfoInSnippets = licenseInfoInSnippets;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public void setSnippetFromFile(String snippetFromFile) {
        this.snippetFromFile = snippetFromFile;
    }

    public Long getId() {
        return id;
    }
}
