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

package eu.occtet.boc.dao;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InformationFileDao {

    private JdbcTemplate jdbcTemplate;
    @Autowired
    public InformationFileDao(@Autowired DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Pair<Long,Float>> findInformationFileContentSimilarity(String text, int limit) {

        String sql = "select *, similarity(file_context,?) as rank " +
                "from INFORMATION_FILE order by rank desc limit ?;";

        List<Map<String, Object>> result= jdbcTemplate.queryForList(sql, text, limit);
        return result.stream()
                .filter(m-> m.get("rank")!=null && (Float)m.get("rank")>0)
                .map(map-> Pair.of((Long)map.get("id"),(Float)map.get("rank")))
                .collect(Collectors.toList());
    }

}
