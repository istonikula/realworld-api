package io.realworld.persistence

import arrow.Kind
import arrow.core.Option
import arrow.core.toOption
import arrow.effects.typeclasses.MonadDefer
import io.realworld.domain.users.User
import io.realworld.domain.users.UserAndPassword
import io.realworld.domain.users.UserId
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.domain.users.ValidUserUpdate
import io.realworld.domain.users.userId
import io.realworld.persistence.Dsl.eq
import io.realworld.persistence.Dsl.insert
import io.realworld.persistence.Dsl.set
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet
import java.util.UUID

open class UserRepository<F>(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
  MD: MonadDefer<F>
) : MonadDefer<F> by MD {

  private fun User.Companion.fromRs(rs: ResultSet) = with(UserTbl) {
    User(
      id = UUID.fromString(rs.getString(id)).userId(),
      email = rs.getString(email),
      token = rs.getString(token),
      username = rs.getString(username),
      bio = rs.getString(bio),
      image = rs.getString(image)
    )
  }

  private fun UserAndPassword.Companion.fromRs(rs: ResultSet) = with(UserTbl) {
    UserAndPassword(User.fromRs(rs), rs.getString(password))
  }

  fun create(user: ValidUserRegistration): Kind<F, User> {
    val sql = with(UserTbl) {
      """
      INSERT INTO $table (
        $id, $email, $token, $username, $password
      ) VALUES (
        :$id, :$email, :$token, :$username, :$password
      )
      RETURNING *
      """
    }
    val params = with(UserTbl) {
      mapOf(
        id to user.id.value,
        email to user.email,
        token to user.token,
        username to user.username,
        password to user.encryptedPassword
      )
    }
    return defer {
      jdbcTemplate.queryForObject(sql, params) { rs, _ -> User.fromRs(rs) }!!.just()
    }
  }

  fun update(update: ValidUserUpdate, current: User): Kind<F, User> {
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

    return defer {
      jdbcTemplate.queryForObject(sql, params) { rs, _ -> User.fromRs(rs) }!!.just()
    }
  }

  fun findById(id: UserId): Kind<F, Option<UserAndPassword>> =
    defer { findByIdEff(id).just() }

  fun findByIdEff(id: UserId): Option<UserAndPassword> =
    DataAccessUtils.singleResult(
      jdbcTemplate.query(
        "SELECT * FROM ${UserTbl.table} WHERE ${UserTbl.id.eq()}",
        mapOf(UserTbl.id to id.value),
        { rs, _ -> UserAndPassword.fromRs(rs) }
      )
    ).toOption()

  fun findByEmail(email: String): Kind<F, Option<UserAndPassword>> =
    defer {
      DataAccessUtils.singleResult(
        jdbcTemplate.query(
          "SELECT * FROM ${UserTbl.table} WHERE ${UserTbl.email.eq()}",
          mapOf(UserTbl.email to email)
        ) { rs, _ -> UserAndPassword.fromRs(rs) }
      ).toOption().just()
    }

  fun findByUsername(username: String): Kind<F, Option<User>> =
    defer {
      DataAccessUtils.singleResult(
        jdbcTemplate.query(
          "SELECT * FROM ${UserTbl.table} WHERE ${UserTbl.username.eq()}",
          mapOf(UserTbl.username to username),
          { rs, _ -> User.fromRs(rs) }
        )
      ).toOption().just()
    }

  open fun existsByEmail(email: String): Kind<F, Boolean> = defer {
    UserTbl.let {
      jdbcTemplate.queryIfExists(it.table, "${it.email.eq()}", mapOf(it.email to email))
    }.just()
  }

  fun existsByUsername(username: String): Kind<F, Boolean> = defer {
    UserTbl.let {
      jdbcTemplate.queryIfExists(it.table, "${it.username.eq()}", mapOf(it.username to username))
    }.just()
  }

  fun hasFollower(followee: UserId, follower: UserId): Kind<F, Boolean> = defer {
    hasFollowerEff(followee, follower).just()
  }

  fun hasFollowerEff(followee: UserId, follower: UserId): Boolean = FollowTbl.let {
    jdbcTemplate.queryIfExists(
      it.table,
      "${it.followee.eq()} AND ${it.follower.eq()}",
      mapOf(it.followee to followee.value, it.follower to follower.value)
    )
  }

  fun addFollower(followee: UserId, follower: UserId): Kind<F, Int> = FollowTbl.let {
    val sql = "${it.table.insert(it.followee, it.follower)} ON CONFLICT (${it.followee}, ${it.follower}) DO NOTHING"
    val params = mapOf(it.followee to followee.value, it.follower to follower.value)
    defer {
      jdbcTemplate.update(sql, params).just()
    }
  }

  fun removeFollower(followee: UserId, follower: UserId): Kind<F, Int> = FollowTbl.let {
    val sql = "DELETE FROM ${it.table} WHERE ${it.followee.eq()} AND ${it.follower.eq()}"
    val params = mapOf(it.followee to followee.value, it.follower to follower.value)
    defer {
      jdbcTemplate.update(sql, params).just()
    }
  }
}
