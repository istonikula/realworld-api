package io.realworld.persistence

import arrow.effects.IO
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

internal fun NamedParameterJdbcTemplate.queryIfExists(
  table: String,
  where: String,
  params: Map<String, Any>
): IO<Boolean> = IO {
  this.queryForObject(
    "SELECT COUNT(*) FROM $table WHERE $where",
    params,
    { rs, _ -> rs.getInt("count") > 0 }
  )!!
}
