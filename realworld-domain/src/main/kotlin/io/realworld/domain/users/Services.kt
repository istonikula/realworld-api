package io.realworld.domain.users

import arrow.Kind
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.effects.typeclasses.MonadDefer
import arrow.typeclasses.binding
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import java.util.UUID

interface ValidateUserService<F> {
  val auth: Auth
  val existsByUsername: ExistsByUsername<F>
  val existsByEmail: ExistsByEmail<F>
  val MD: MonadDefer<F>

  fun UserRegistration.validate(): Kind<F, Either<UserRegistrationError, ValidUserRegistration>> {
    val cmd = this
    return MD.binding {
      when {
        existsByEmail(cmd.email).bind() ->
          UserRegistrationError.EmailAlreadyTaken.left()
        existsByUsername(cmd.username).bind() ->
          UserRegistrationError.UsernameAlreadyTaken.left()
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
}

interface ValidateUserUpdateService<F> {
  val auth: Auth
  val existsByUsername: ExistsByUsername<F>
  val existsByEmail: ExistsByEmail<F>
  val MD: MonadDefer<F>

  fun UserUpdate.validate(current: User): Kind<F, Either<UserUpdateError, ValidUserUpdate>> {
    val cmd = this
    return MD.binding {
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
    }
  }
}
