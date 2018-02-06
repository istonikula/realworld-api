package io.realworld

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonRootName
import io.realworld.domain.api.*
import io.realworld.domain.api.dto.UserDto
import io.realworld.domain.api.event.LoginEvent
import io.realworld.domain.api.event.RegisterEvent
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

@JsonRootName("user")
data class Login(
  @field:Email
  @field:NotBlank
  val email: String,

  @field:NotBlank
  val password: String
)

@JsonRootName("user")
data class Registration(
  @field:NotBlank
  val username: String,

  @field:Email
  @field:NotBlank
  val email: String,

  @field:NotBlank
  val password: String
)

@JsonRootName("user")
data class UserUpdate(
  @field:Email
  val email: String? = null,

  val username: String? = null,
  val password: String? = null,
  val bio: String? = null,
  val image: String? = null
)

@JsonRootName("user")
data class User(
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
) {
  companion object {
    fun fromDto(dto: UserDto) = UserMappers.user.mapReverse(dto)
  }
}

@RestController
class UserController(
  private val userService: UserService,
  private val registerUser: RegisterUser
) {

  @GetMapping("/api/user")
  fun currentUser(user: UserDto) = ResponseEntity.ok().body(User.fromDto(user))

  @PostMapping("/api/users")
  fun register(@Valid @RequestBody registration: Registration): ResponseEntity<User> {
    val e = registerUser(RegisterUserCommand(UserRegistration(
      username = registration.username,
      email = registration.email,
      password = registration.password
    )))
      .unsafeRunSync()

    return e.fold({
        when (it) {
          is UserRegistrationValidationError.EmailAlreadyTaken ->
            throw FieldError("email", "already taken")
          is UserRegistrationValidationError.UsernameAlreadyTaken ->
            throw FieldError("username", "already taken")
        }
      },
      { ResponseEntity.status(HttpStatus.CREATED).body(User.fromDto(it.user)) }
    )
  }

  @PostMapping("/api/users/login")
  fun login(@Valid @RequestBody login: Login): ResponseEntity<User> {
    val e = userService.login(LoginEvent(
      email = login.email,
      password = login.password
    ))
    return ResponseEntity.ok().body(User.fromDto(e.user))
  }

  @PutMapping("/api/user")
  fun update(@Valid @RequestBody userUpdate: UserUpdate, user: UserDto): ResponseEntity<User> {
    return ResponseEntity.ok().body(User.fromDto(user))
  }
}

object UserMappers {
  val user = OrikaBeanMapper.FACTORY.getMapperFacade(User::class.javaObjectType, UserDto::class.javaObjectType)
}
