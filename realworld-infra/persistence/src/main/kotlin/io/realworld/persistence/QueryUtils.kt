package io.realworld.persistence

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

object Dsl {
  fun String.eq() = "$this = :$this"
  fun String.insert(vararg cols: String) =
    "INSERT INTO $this (${cols.joinToString()}) VALUES (${cols.joinToString { ":$it" }})"
  fun String.now() = "$this = CURRENT_TIMESTAMP"
  fun String.set() = "$this = :$this"
}

internal suspend fun NamedParameterJdbcTemplate.queryIfExists(
  table: String,
  where: String,
  params: Map<String, Any>
): Boolean = queryForObject("SELECT COUNT(*) FROM $table WHERE $where", params) { rs, _ -> rs.getInt("count") > 0 }!!
