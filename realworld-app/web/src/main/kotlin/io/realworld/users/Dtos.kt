package io.realworld.users

import com.fasterxml.jackson.annotation.JsonRootName
import io.realworld.domain.users.User
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

@JsonRootName("user")
data class LoginDto(
  @field:Email
  @field:NotBlank
  val email: String,

  @field:NotBlank
  val password: String
)

@JsonRootName("user")
data class RegistrationDto(
  @field:NotBlank
  val username: String,

  @field:Email
  @field:NotBlank
  val email: String,

  @field:NotBlank
  val password: String
)

@JsonRootName("user")
data class UserUpdateDto(
  @field:Email
  val email: String? = null,

  val username: String? = null,
  val password: String? = null,
  val bio: String? = null,
  val image: String? = null
)

data class UserResponseDto(
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
) {
  companion object {
    fun fromDomain(domain: User) = with(domain) {
      UserResponseDto(email = email, token = token, username = username, bio = bio, image = image)
    }
  }
}
