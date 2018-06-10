package io.realworld.persistence

import arrow.core.Option
import arrow.core.toOption
import arrow.effects.IO
import io.realworld.domain.users.User
import io.realworld.domain.users.UserAndPassword
import io.realworld.domain.users.UserRepository
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.domain.users.ValidUserUpdate
import io.realworld.persistence.UserTbl.email
import io.realworld.persistence.UserTbl.eq
import io.realworld.persistence.UserTbl.username
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

  override fun create(user: ValidUserRegistration): IO<User> {
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
    return IO {
      jdbcTemplate.queryForObject(sql, params, { rs, _ -> User.fromRs(rs) })!!
    }
  }

  override fun update(update: ValidUserUpdate, current: User): IO<User> {
    val sql = with(UserTbl) {
      StringBuilder("UPDATE $table SET ${username.set()}, ${email.set()}, ${bio.set()}, ${image.set()}")
        .also { if (update.encryptedPassword.isDefined()) it.append(", ${password.set()}") }
        .also { it.append(" WHERE $email = :currentEmail RETURNING *") }
        .toString()
    }
    val params = with(UserTbl) {
      mapOf(
        username to update.username,
        email to update.email,
        bio to update.bio,
        image to update.image,
        password to update.encryptedPassword.orNull(),
        "currentEmail" to current.email
      )
    }

    return IO {
      jdbcTemplate.queryForObject(sql, params, { rs, _ -> User.fromRs(rs) })!!
    }
  }

  override fun findByEmail(email: String): Option<UserAndPassword> = DataAccessUtils.singleResult(
    jdbcTemplate.query(
      "SELECT * FROM ${UserTbl.table} WHERE ${UserTbl.email.eq()}",
      mapOf(UserTbl.email to email),
      { rs, _ -> UserAndPassword.fromRs(rs) }
    )
  ).toOption()

  override fun existsByEmail(email: String): Boolean = UserTbl.let {
    queryIfExists(it.table, "${it.email.eq()}", mapOf(it.email to email))
  }

  override fun existsByUsername(username: String): Boolean = UserTbl.let {
    queryIfExists(it.table, "${it.username.eq()}", mapOf(it.username to username))
  }

  private fun queryIfExists(table: String, where: String, params: Map<String, Any>): Boolean =
    jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM $table WHERE $where",
      params,
      { rs, _ -> rs.getInt("count") > 0 }
    )!!
}
