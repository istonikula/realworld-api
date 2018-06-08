package io.realworld

import com.fasterxml.jackson.annotation.JsonRootName
import io.realworld.domain.api.LoginUserCommand
import io.realworld.domain.api.RegisterUserCommand
import io.realworld.domain.api.User
import io.realworld.domain.api.UserRegistration
import io.realworld.domain.api.UserRegistrationValidationError
import io.realworld.domain.core.Auth
import io.realworld.domain.core.GetUserSyntax
import io.realworld.domain.core.LoginUserWorkflowSyntax
import io.realworld.domain.core.RegisterUserWorkflowSyntax
import io.realworld.domain.core.SaveUserSyntax
import io.realworld.domain.core.ValidateUserSyntax
import io.realworld.domain.spi.GetUser
import io.realworld.domain.spi.SaveUser
import io.realworld.domain.spi.UserRepository
import io.realworld.domain.spi.ValidateUserRegistration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
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

data class UserDto(
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
) {
  companion object {
    fun fromDomain(domain: User) = with(domain) {
      UserDto(email = email, token = token, username = username, bio = bio, image = image)
    }
  }
}

data class UserResponse(val user: UserDto) {
  companion object {
    fun fromDomain(domain: User) = UserResponse(UserDto.fromDomain(domain))
  }
}

@RestController
class UserController(
  private val auth0: Auth,
  private val userRepository0: UserRepository
) {

  @GetMapping("/api/user")
  fun currentUser(user: User) = ResponseEntity.ok().body(UserResponse.fromDomain(user))

  @PostMapping("/api/users")
  fun register(@Valid @RequestBody registration: RegistrationDto): ResponseEntity<UserResponse> {
    val validateUserSyntax = object : ValidateUserSyntax { override val userRepository = userRepository0 }
    val saveUserSyntax = object : SaveUserSyntax { override val userRepository = userRepository0 }

    val workflowSyntax = object: RegisterUserWorkflowSyntax {
      override val auth = auth0
      override val saveUser: SaveUser = { x -> saveUserSyntax.run { x.save() } }
      override val validateUser: ValidateUserRegistration = { x -> validateUserSyntax.run { x.validate() } }
    }

    return workflowSyntax.run {
      RegisterUserCommand(UserRegistration(
        username = registration.username,
        email = registration.email,
        password = registration.password
      )).registerUser()
    }
      .unsafeRunSync()
      .fold(
        {
          when (it) {
            is UserRegistrationValidationError.EmailAlreadyTaken ->
              throw FieldError("email", "already taken")
            is UserRegistrationValidationError.UsernameAlreadyTaken ->
              throw FieldError("username", "already taken")
          }
        },
        { ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromDomain(it.user)) }
      )
  }

  @PostMapping("/api/users/login")
  fun login(@Valid @RequestBody login: LoginDto): ResponseEntity<UserResponse> {
    val getUserSyntax = object : GetUserSyntax {
      override val userRepository = userRepository0
    }

    val loginWorkflowSyntax = object : LoginUserWorkflowSyntax {
      override val auth = auth0
      override val getUser: GetUser = { x -> getUserSyntax.run { x.getUser() } }
    }

    return loginWorkflowSyntax.run {
      LoginUserCommand(
        email = login.email,
        password = login.password
      ).loginUser()
    }
      .unsafeRunSync()
      .fold(
        { throw UnauthrorizedException() },
        { ResponseEntity.ok().body(UserResponse.fromDomain(it.user)) })
  }

  @PutMapping("/api/user")
  fun update(@Valid @RequestBody userUpdate: UserUpdateDto, user: User): ResponseEntity<UserResponse> =
    ResponseEntity.ok().body(UserResponse.fromDomain(user))
}
