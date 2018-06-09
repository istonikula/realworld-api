package io.realworld.persistence

import io.realworld.domain.users.User
import io.realworld.domain.users.UserAndPassword
import io.realworld.domain.users.UserRepository
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.persistence.UserTbl.eq
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet

open class JdbcUserRepository(val jdbcTemplate: NamedParameterJdbcTemplate) : UserRepository {

  fun User.Companion.fromRs(rs: ResultSet) = with(UserTbl) {
    User(
      email = rs.getString(email),
      token = rs.getString(token),
      username = rs.getString(username),
      bio = rs.getString(bio),
      image = rs.getString(image)
    )
  }

  fun UserAndPassword.Companion.fromRs(rs: ResultSet) = with(UserTbl) {
    UserAndPassword(User.fromRs(rs), rs.getString(password))
  }

  override fun create(user: ValidUserRegistration): User {
    val sql = with(UserTbl) {
      """
      INSERT INTO $table (
        $email, $token, $username, $password
      ) VALUES (
        :$email, :$token, :$username, :$password
      )
      RETURNING *
      """
    }
    val params = with(UserTbl) {
      mapOf(
        email to user.email,
        token to user.token,
        username to user.username,
        password to user.encryptedPassword
      )
    }
    return jdbcTemplate.queryForObject(sql, params, { rs, _ -> User.fromRs(rs) })!!
  }

  override fun update(user: User): User {
    TODO("not implemented")
  }

  override fun findByEmail(email: String): UserAndPassword? = DataAccessUtils.singleResult(
    jdbcTemplate.query(
      "SELECT * FROM ${UserTbl.table} WHERE ${UserTbl.email.eq()}",
      mapOf(UserTbl.email to email),
      { rs, _ -> UserAndPassword.fromRs(rs) }
    )
  )

  override fun existsByEmail(byEmail: String): Boolean = with(UserTbl) {
    queryIfExists(table, "${email.eq()}", mapOf(email to byEmail))
  }

  override fun existsByUsername(byUsername: String): Boolean = with(UserTbl) {
    queryIfExists(table, "${username.eq()}", mapOf(username to byUsername))
  }

  private fun queryIfExists(table: String, where: String, params: Map<String, Any>): Boolean =
    jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM ${table} WHERE $where",
      params,
      { rs, _ -> rs.getInt("count") > 0 }
    )!!
}
