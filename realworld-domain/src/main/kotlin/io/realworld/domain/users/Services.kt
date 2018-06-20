package io.realworld.domain.users

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.extensions
import arrow.effects.fix
import arrow.typeclasses.binding
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import java.util.UUID

interface ValidateUserService {
  val auth: Auth
  val existsByUsername: ExistsByUsername
  val existsByEmail: ExistsByEmail

  fun UserRegistration.validate(): IO<Either<UserRegistrationError, ValidUserRegistration>> {
    val cmd = this
    return ForIO extensions {
      binding {
        when {
          existsByEmail(cmd.email).bind() ->
            UserRegistrationError.EmailAlreadyTaken.left()
          existsByUsername(cmd.username).bind() ->
            UserRegistrationError.UsernameAlreadyTaken.left()
          else -> {
            val id = UUID.randomUUID()
            ValidUserRegistration(
              id = id,
              email = email,
              username = username,
              token = auth.createToken(Token(id)),
              encryptedPassword = auth.encryptPassword(password)
            ).right()
          }
        }
      }.fix()
    }
  }
}

interface ValidateUserUpdateService {
  val auth: Auth
  val existsByUsername: ExistsByUsername
  val existsByEmail: ExistsByEmail

  fun UserUpdate.validate(current: User): IO<Either<UserUpdateError, ValidUserUpdate>> {
    val cmd = this
    return ForIO extensions {
      binding {
        when {
          cmd.email.fold({ false }, { current.email !== it && existsByEmail(it).bind() }) ->
            UserUpdateError.EmailAlreadyTaken.left()
          cmd.username.fold({ false }, { current.username !== it && existsByUsername(it).bind() }) ->
            UserUpdateError.UsernameAlreadyTaken.left()
          else -> ValidUserUpdate(
            email = email.getOrElse { current.email },
            username = username.getOrElse { current.username },
            encryptedPassword = password.map { auth.encryptPassword(it) },
            bio = bio.getOrElse { current.bio },
            image = image.getOrElse { current.image }
          ).right()
        }
      }.fix()
    }
  }
}
