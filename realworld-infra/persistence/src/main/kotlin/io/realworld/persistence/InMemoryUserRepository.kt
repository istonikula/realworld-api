package io.realworld.persistence

import io.realworld.domain.spi.UserModel
import io.realworld.domain.spi.UserRepository

open class InMemoryUserRepository : UserRepository {
  private val byEmail: MutableMap<String, UserModel> = mutableMapOf()
  private val byUsername: MutableMap<String, UserModel> = mutableMapOf()

  override fun findByEmail(email: String): UserModel? {
    val userModel = byEmail[email]
    return userModel
  }

  override fun save(user: UserModel): UserModel {
    byEmail[user.email] = user
    byUsername[user.username] = user
    return user
  }

  override fun existsByEmail(email: String) = byEmail[email] != null
  override fun existsByUsername(username: String) = byUsername[username] != null

  fun deleteAll() {
    byEmail.clear()
    byUsername.clear()
  }
}
