package io.realworld.domain.core

import arrow.core.Either
import arrow.data.EitherT
import arrow.data.monad
import arrow.data.value
import arrow.effects.IO
import arrow.effects.IOHK
import arrow.effects.ev
import arrow.typeclasses.binding
import io.realworld.domain.api.*
import io.realworld.domain.spi.SaveUser
import io.realworld.domain.spi.UserModel
import io.realworld.domain.spi.UserRepository
import io.realworld.domain.spi.ValidateUserRegistration

class RegisterUserWorkflow(
  val auth: Auth,
  val validateUserRegistration: ValidateUserRegistration,
  val saveUser: SaveUser
) : RegisterUser {
  override fun invoke(cmd: RegisterUserCommand): IO<Either<UserRegistrationValidationError, RegisterUserAcknowledgment>> =
    EitherT.monad<IOHK, UserRegistrationValidationError>().binding() {
      // TODO all this needs to be run inside db transaction
      val validRegistration = EitherT(validateUserRegistration(cmd.data)).bind()
      val savedUser = EitherT(saveUser(UserModel(
        email = validRegistration.email,
        username = validRegistration.username,
        password = auth.encryptPassword(validRegistration.password),
        token = auth.createToken(Token(validRegistration.email))
      ))).bind()
      RegisterUserAcknowledgment(savedUser.toDto())
    }.value().ev()
  }

class ValidateUserRegistrationBean(
  val userRepository: UserRepository
) : ValidateUserRegistration {
  override fun invoke(reg: UserRegistration): IO<Either<UserRegistrationValidationError, UserRegistration>> =
    IO {
      when {
        userRepository.existsByEmail(reg.email) -> Either.left(UserRegistrationValidationError.EmailAlreadyTaken)
        userRepository.existsByUsername(reg.username) -> Either.left(UserRegistrationValidationError.UsernameAlreadyTaken)
        else -> Either.right(reg)
      }
    }
}

class SaveUserBean(
  val userRepository: UserRepository
) : SaveUser {
  override fun invoke(model: UserModel): IO<Either<UserRegistrationValidationError, UserModel>> =
    IO { Either.right(userRepository.save(model)) }
}
