package io.realworld.users

import arrow.core.Option
import io.realworld.domain.common.Auth
import io.realworld.domain.users.CreateUser
import io.realworld.domain.users.GetUserByEmail
import io.realworld.domain.users.LoginUserCommand
import io.realworld.domain.users.LoginUserUseCase
import io.realworld.domain.users.RegisterUserCommand
import io.realworld.domain.users.RegisterUserUseCase
import io.realworld.domain.users.UpdateUser
import io.realworld.domain.users.UpdateUserCommand
import io.realworld.domain.users.UpdateUserUseCase
import io.realworld.domain.users.User
import io.realworld.domain.users.UserRegistration
import io.realworld.domain.users.UserUpdate
import io.realworld.domain.users.ValidateUserRegistration
import io.realworld.domain.users.ValidateUserService
import io.realworld.domain.users.ValidateUserUpdate
import io.realworld.domain.users.ValidateUserUpdateService
import io.realworld.errors.toRestException
import io.realworld.persistence.UserRepository
import io.realworld.runWriteTx
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

data class UserResponse(val user: UserResponseDto) {
  companion object {
    fun fromDomain(domain: User) = UserResponse(UserResponseDto.fromDomain(domain))
  }
}

@RestController
class UserController(
  private val auth0: Auth,
  private val repo: UserRepository,
  private val txManager: PlatformTransactionManager
) {

  @GetMapping("/api/user")
  fun currentUser(user: User) = ResponseEntity.ok().body(UserResponse.fromDomain(user))

  @PostMapping("/api/users")
  fun register(@Valid @RequestBody registration: RegistrationDto): ResponseEntity<UserResponse> {
    val validateUserSrv = object : ValidateUserService {
      override val auth = auth0
      override val existsByEmail = repo::existsByEmail
      override val existsByUsername = repo::existsByUsername
    }

    return object : RegisterUserUseCase {
      override val createUser: CreateUser = repo::create
      override val validateUser: ValidateUserRegistration = { x -> validateUserSrv.run { x.validate() } }
    }.run {
      RegisterUserCommand(UserRegistration(
        username = registration.username,
        email = registration.email,
        password = registration.password
      )).runUseCase()
    }.runWriteTx(txManager).fold(
      { it.toRestException() },
      { ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromDomain(it)) }
    )
  }

  @PostMapping("/api/users/login")
  fun login(@Valid @RequestBody login: LoginDto): ResponseEntity<UserResponse> {
    return object : LoginUserUseCase {
      override val auth = auth0
      override val getUser: GetUserByEmail = repo::findByEmail
    }.run {
      LoginUserCommand(
        email = login.email,
        password = login.password
      ).runUseCase()
    }.runWriteTx(txManager).fold(
      { it.toRestException() },
      { ResponseEntity.ok().body(UserResponse.fromDomain(it)) }
    )
  }

  @PutMapping("/api/user")
  fun update(@Valid @RequestBody update: UserUpdateDto, user: User): ResponseEntity<UserResponse> {
    val validateUpdateSrv = object : ValidateUserUpdateService {
      override val auth = auth0
      override val existsByEmail = repo::existsByEmail
      override val existsByUsername = repo::existsByUsername
    }

    return object : UpdateUserUseCase {
      override val validateUpdate: ValidateUserUpdate = { x, y -> validateUpdateSrv.run { x.validate(y) } }
      override val updateUser: UpdateUser = repo::update
    }.run {
      UpdateUserCommand(
        data = UserUpdate(
          username = Option.fromNullable(update.username),
          email = Option.fromNullable(update.email),
          password = Option.fromNullable(update.password),
          bio = Option.fromNullable(update.bio),
          image = Option.fromNullable(update.image)
        ),
        current = user
      ).runUseCase()
    }.runWriteTx(txManager).fold(
      { it.toRestException() },
      { ResponseEntity.ok(UserResponse.fromDomain(it)) }
    )
  }
}
