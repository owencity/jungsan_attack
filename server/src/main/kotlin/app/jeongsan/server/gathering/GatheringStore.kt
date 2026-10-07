package app.jeongsan.server.gathering

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

/** 스키마의 진실은 016-independent-settlement-units.yaml. 단위 잠금과 배치 조회를 명시하려 JDBC를 쓴다. */
@Repository
class GatheringStore(source: DataSource) {
    private val jdbc = NamedParameterJdbcTemplate(source)
    private fun params(values: Array<out Pair<String, Any?>>) = MapSqlParameterSource().apply {
        values.forEach { (key, value) -> addValue(key, if (value is Instant) Timestamp.from(value) else value) }
    }
    fun rows(sql: String, vararg values: Pair<String, Any?>): List<Map<String, Any?>> = jdbc.queryForList(sql, params(values))
    fun update(sql: String, vararg values: Pair<String, Any?>): Int = jdbc.update(sql, params(values))
    fun insert(sql: String, vararg values: Pair<String, Any?>): Long {
        val key = GeneratedKeyHolder()
        jdbc.update(sql, params(values), key, arrayOf("id"))
        return key.key!!.toLong()
    }
}

internal fun Map<String, Any?>.long(key: String): Long = (getValue(key) as Number).toLong()
internal fun Map<String, Any?>.text(key: String): String = getValue(key).toString()
internal fun Map<String, Any?>.instant(key: String): Instant? = when (val v = get(key)) {
    is Timestamp -> v.toInstant()
    is java.time.LocalDateTime -> v.toInstant(java.time.ZoneOffset.UTC)
    else -> null
}
