package io.realworld.domain.users

import java.util.UUID

@JvmInline value class UserId(val value: UUID)
fun UUID.userId() = UserId(this)

data class User(
  val id: UserId,
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
) { companion object }

data class UserRegistration(val username: String, val email: String, val password: String)

data class ValidUserRegistration(
  val id: UserId,
  val email: String,
  val token: String,
  val username: String,
  val encryptedPassword: String
)

data class UserAndPassword(val user: User, val encryptedPassword: String) { companion object }

data class UserUpdate(
  val username: String?,
  val email: String?,
  val password: String?,
  val bio: String?,
  val image: String?
)

data class ValidUserUpdate(
  val username: String,
  val email: String,
  val encryptedPassword: String?,
  val bio: String?,
  val image: String?
)
