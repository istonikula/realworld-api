package io.realworld.domain.users

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import java.util.UUID

interface ValidateUserService {
  val auth: Auth
  val existsByUsername: ExistsByUsername
  val existsByEmail: ExistsByEmail

  suspend fun UserRegistration.validate(): Either<UserRegistrationError, ValidUserRegistration> {
    val cmd = this

    return when {
      existsByEmail(cmd.email) -> UserRegistrationError.EmailAlreadyTaken.left()
      existsByUsername(cmd.username) -> UserRegistrationError.UsernameAlreadyTaken.left()
      else -> {
        val id = UUID.randomUUID().userId()
        ValidUserRegistration(
          id = id,
          email = email,
          username = username,
          token = auth.createToken(Token(id)),
          encryptedPassword = auth.encryptPassword(password)
        ).right()
      }
    }
  }
}

interface ValidateUserUpdateService {
  val auth: Auth
  val existsByUsername: ExistsByUsername
  val existsByEmail: ExistsByEmail

  suspend fun UserUpdate.validate(current: User): Either<UserUpdateError, ValidUserUpdate> {
    val cmd = this

    return when {
      cmd.email.fold({ false }, { current.email !== it && existsByEmail(it) }) ->
        UserUpdateError.EmailAlreadyTaken.left()
      cmd.username.fold({ false }, { current.username !== it && existsByUsername(it) }) ->
        UserUpdateError.UsernameAlreadyTaken.left()
      else -> ValidUserUpdate(
        email = email.getOrElse { current.email },
        username = username.getOrElse { current.username },
        encryptedPassword = password.map { auth.encryptPassword(it) },
        bio = bio.getOrElse { current.bio },
        image = image.getOrElse { current.image }
      ).right()
    }
  }
}
