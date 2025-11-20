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

@Embeddable
public class CrossRef {
    @Column(nullable = false)
    private String url;

    private Boolean isLive;

    private Boolean isValid;

    private Boolean isWayBackLink;

    private String match;

    private Integer order;

    private String timestamp;

    public Boolean getLive() {
        return isLive;
    }

    public Boolean getValid() {
        return isValid;
    }

    public Boolean getWayBackLink() {
        return isWayBackLink;
    }

    public Integer getOrder() {
        return order;
    }

    public String getMatch() {
        return match;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }

    public void setLive(Boolean live) {
        isLive = live;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setValid(Boolean valid) {
        isValid = valid;
    }

    public void setWayBackLink(Boolean wayBackLink) {
        isWayBackLink = wayBackLink;
    }
}
