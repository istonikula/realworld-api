package io.realworld.domain.core

import arrow.core.Either
import arrow.data.EitherT
import arrow.data.monad
import arrow.data.value
import arrow.effects.IO
import arrow.effects.IOHK
import arrow.effects.ev
import arrow.effects.functor
import arrow.syntax.either.right
import arrow.typeclasses.binding
import io.realworld.domain.api.*
import io.realworld.domain.spi.*

class RegisterUserWorkflow(
  val auth: Auth,
  val validateUserRegistration: ValidateUserRegistration,
  val saveUser: SaveUser
): RegisterUser {
  override fun invoke(cmd: RegisterUserCommand): IO<Either<UserRegistrationValidationError, RegisterUserAcknowledgment>> =
    EitherT.monad<IOHK, UserRegistrationValidationError>().binding() {
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

class LoginUserWorkflow(
  val auth: Auth,
  val getUser: GetUser
): LoginUser {
  override fun invoke(cmd: LoginUserCommand): IO<Either<UserLoginError, LoginUserAcknowledgment>> =
    EitherT.monad<IOHK, UserLoginError>().binding() {
      val user = EitherT(getUser(cmd.email)).mapLeft({ UserLoginError.BadCredentials }, IO.functor()).bind()
      auth.checkPassword(cmd.password, user.password)
      EitherT(IO.pure(
        when (auth.checkPassword(cmd.password, user.password)) {
          true -> LoginUserAcknowledgment(user.toDto()).right()
          false -> Either.left(UserLoginError.BadCredentials)
        }
      )).bind()
    }.value().ev()
}

class ValidateUserRegistrationBean(
  val userRepository: UserRepository
): ValidateUserRegistration {
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
): SaveUser {
  override fun invoke(model: UserModel): IO<Either<UserRegistrationValidationError, UserModel>> =
    IO { Either.right(userRepository.save(model)) }
}

class GetUserBean(
  val userRepository: UserRepository
) : GetUser {
  override fun invoke(email: String): IO<Either<UserNotFound, UserModel>> =
    IO {userRepository.findByEmail(email)?.let { it.right() } ?: Either.left(UserNotFound()) }
}
