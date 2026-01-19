package eu.occtet.bocfrontend.dao;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LicenseDao {

    private JdbcTemplate jdbcTemplate;
    @Autowired
    public LicenseDao(@Autowired DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Pair<Long,Float>> findLicenseIdsSimilarTo(String text, int limit) {

        String sql = "select *, similarity(license_text,?) as rank " +
                "from LICENSE order by rank desc limit ?;";

        List<Map<String, Object>> result= jdbcTemplate.queryForList(sql, text, limit);
        return result.stream()
                .filter(m-> (Float)m.get("rank")!=null && (Float)m.get("rank")>0)
                .map(map-> Pair.of((long)map.get("id"),(Float)map.get("rank")))
                .collect(Collectors.toList());
    }

}
