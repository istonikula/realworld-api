package io.realworld.domain.users

import arrow.core.Option
import java.util.UUID

// TODO replace with inline class when kotlin 1.3 comes out
data class UserId(val value: UUID)
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
  val username: Option<String>,
  val email: Option<String>,
  val password: Option<String>,
  val bio: Option<String>,
  val image: Option<String>
)

data class ValidUserUpdate(
  val username: String,
  val email: String,
  val encryptedPassword: Option<String>,
  val bio: String?,
  val image: String?
)
