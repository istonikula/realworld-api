package io.realworld.persistence

import io.realworld.domain.users.User
import java.util.UUID

data class UserModel(
  val id: UUID,
  val email: String,
  val token: String,
  val username: String,
  val password: String,
  val bio: String? = null,
  val image: String? = null
) {
  fun toDomain() = User(email = email, token = token, username = username, bio = bio, image = image)
}

private interface KtlintDisableFilenameRule
