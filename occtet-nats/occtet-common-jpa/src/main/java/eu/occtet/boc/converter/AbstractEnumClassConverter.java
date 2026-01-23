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

package eu.occtet.boc.converter;

import eu.occtet.boc.entity.appconfigurations.EnumClass;
import jakarta.persistence.AttributeConverter;

public abstract class AbstractEnumClassConverter<E extends Enum<E> & EnumClass<String>>
        implements AttributeConverter<E, String> {

    private final Class<E> enumClass;

    protected AbstractEnumClassConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return (attribute == null) ? null : attribute.getId();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // Generic lookup: iterates all constants to find the matching ID
        for (E e : enumClass.getEnumConstants()) {
            if (e.getId().equals(dbData)) {
                return e;
            }
        }
        return null;
    }
}