/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.download.config;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import java.util.Map;

@Configuration
public class CustomizedEclipseLinkJpaVendorAdapter extends EclipseLinkJpaVendorAdapter {


    @Value("${spring.jpa.generate-ddl}")
    private boolean generateDdl;

    @Override
    public Map<String, Object> getJpaPropertyMap() {
        Map<String, Object> map= super.getJpaPropertyMap();
        map.put(PersistenceUnitProperties.WEAVING, "false");
        if(generateDdl)
            map.put(PersistenceUnitProperties.DDL_GENERATION, "create-or-extend-tables");
        else
            map.put(PersistenceUnitProperties.DDL_GENERATION, "none");

        return map;
    }

}
