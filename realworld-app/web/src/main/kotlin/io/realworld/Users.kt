package io.realworld

import com.fasterxml.jackson.annotation.JsonRootName
import io.realworld.domain.api.UserService
import io.realworld.domain.api.dto.UserDto
import io.realworld.domain.api.event.LoginEvent
import io.realworld.domain.api.event.RegisterEvent
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
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

  @GetMapping
  fun currentUser(user: UserDto) = ResponseEntity.ok().body(User.fromDto(user))

  @PostMapping
  fun register(@Valid @RequestBody registration: Registration): ResponseEntity<User> {
    val e = userService.register(RegisterEvent(
      username = registration.username,
      email = registration.email,
      password = registration.password
    ))
    return ResponseEntity.ok().body(User.fromDto(e.user)) // TODO return 201 instead of 200
  }

  @PostMapping("/login")
  fun login(@Valid @RequestBody login: Login): ResponseEntity<User> {
    val e = userService.login(LoginEvent(
      email = login.email,
      password = login.password
    ))
    return ResponseEntity.ok().body(User.fromDto(e.user))
  }
}

object UserMappers {
  val user = OrikaBeanMapper.FACTORY.getMapperFacade(User::class.javaObjectType, UserDto::class.javaObjectType)
}
