package io.realworld.domain.core

import arrow.core.*
import arrow.typeclasses.binding
import io.realworld.domain.api.*
import io.realworld.domain.spi.UserModel
import io.realworld.domain.spi.UserRepository
import io.realworld.domain.spi.ValidateUserRegistration

class RegisterUserWorkflow(
  val auth: Auth,
  val userRepository: UserRepository,
  val validateUserRegistration: ValidateUserRegistration
) : RegisterUser {
  override fun invoke(cmd: RegisterUserCommand): Either<UserRegistrationValidationError, RegisterUserAcknowledgment> =
    Either.monad<UserRegistrationValidationError>().binding() {
      // TODO all this needs to be run inside db transaction
      val validRegistration = validateUserRegistration(cmd.data).bind()
      val savedUser = userRepository.save(UserModel(
        email = validRegistration.email,
        username = validRegistration.username,
        password = auth.encryptPassword(validRegistration.password),
        token = auth.createToken(Token(validRegistration.email))
      ))
      RegisterUserAcknowledgment(savedUser.toDto())
    }.ev()
}

// TODO this should return IO
class ValidateUserRegistrationBean(
  val userRepository: UserRepository
) : ValidateUserRegistration {
  override fun invoke(reg: UserRegistration): Either<UserRegistrationValidationError, UserRegistration> =
    when {
      userRepository.existsByEmail(reg.email) -> Either.left(UserRegistrationValidationError.EmailAlreadyTaken)
      userRepository.existsByUsername(reg.username) -> Either.left(UserRegistrationValidationError.UsernameAlreadyTaken)
      else -> Either.right(reg)
    }
}
