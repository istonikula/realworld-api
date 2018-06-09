package io.realworld.domain.users

data class User(
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
) { companion object }

data class UserRegistration(val username: String, val email: String, val password: String)

data class ValidUserRegistration(
  val email: String,
  val token: String,
  val username: String,
  val encryptedPassword: String
)

data class UserAndPassword(val user: User, val encryptedPassword: String) { companion object }
