package io.realworld.persistence

import io.realworld.domain.spi.UserModel
import io.realworld.domain.spi.UserRepository

class InMemoryUserRepository : UserRepository {

  private val usersByEmail: MutableMap<String, UserModel> = mutableMapOf()

  override fun findByEmail(email: String): UserModel? {
    val userModel = usersByEmail[email]
    return userModel
  }

  override fun save(user: UserModel): UserModel {
    usersByEmail[user.email] = user
    return user
  }
}
