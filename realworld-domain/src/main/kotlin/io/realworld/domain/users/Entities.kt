package io.realworld.domain.users

data class User(
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
)

data class UserRegistration(val username: String, val email: String, val password: String)

// TODO move to infra
data class UserModel(
  val email: String,
  val token: String,
  val username: String,
  val password: String,
  val bio: String? = null,
  val image: String? = null
) {
  fun toDomain() = User(email = email, token = token, username = username, bio = bio, image = image)
}
