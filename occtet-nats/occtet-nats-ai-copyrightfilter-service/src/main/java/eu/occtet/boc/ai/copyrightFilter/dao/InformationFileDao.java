package eu.occtet.boc.ai.copyrightFilter.dao;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InformationFileDao {

    private JdbcTemplate jdbcTemplate;
    @Autowired
    public InformationFileDao(@Autowired DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Pair<UUID,Float>> findInformationFileContentSimilarity(String text, int limit) {

        String sql = "select *, similarity(file_context,?) as rank " +
                "from INFORMATION_FILE order by rank desc limit ?;";

        List<Map<String, Object>> result= jdbcTemplate.queryForList(sql, text, limit);
        return result.stream()
                .filter(m-> m.get("rank")!=null && (Float)m.get("rank")>0)
                .map(map-> Pair.of((UUID)map.get("id"),(Float)map.get("rank")))
                .collect(Collectors.toList());
    }

}
