package io.realworld.domain.api.dto

data class UserDto(
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
)
