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

package eu.occtet.bocfrontend.entity;

/**
 * Defines a contract for entities that are strictly bound to a specific tenant or organization.
 * <p>
 * Implementing this interface allows backend listeners to automatically manage organizational
 * assignment during the entity lifecycle, ensuring consistent data isolation across the platform.
 */
public interface HasOrganization {

    /**
     * Retrieves the organization that owns this entity.
     *
     * @return the assigned organization
     */
    Organization getOrganization();

    /**
     * Assigns the specified organization as the owner of this entity.
     *
     * @param organization the organization to assign
     */
    void setOrganization(Organization organization);
}
