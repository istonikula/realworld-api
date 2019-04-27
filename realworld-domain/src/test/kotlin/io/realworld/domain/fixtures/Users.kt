package io.realworld.domain.fixtures

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import arrow.core.some
import arrow.core.toOption
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import io.realworld.domain.users.User
import io.realworld.domain.users.UserAndPassword
import io.realworld.domain.users.UserId
import io.realworld.domain.users.UserRegistration
import io.realworld.domain.users.UserUpdate
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.domain.users.ValidUserUpdate
import io.realworld.domain.users.userId
import java.util.UUID

class UserFactory(val auth: Auth) {
  fun createUser(username: String, id: UserId= UUID.randomUUID().userId()) = User(
    id = id,
    email = "$username@realworld.io",
    token = auth.createToken(Token(id)),
    username = username,
    bio = null,
    image = null
  )

  fun UserRegistration.valid(id: UserId = UUID.randomUUID().userId()) = ValidUserRegistration(
    id = id,
    email = email,
    token = auth.createToken(Token(id)),
    username = username,
    encryptedPassword = auth.encryptPassword(password)
  )

  val validRegistration =
    { x: UserRegistration -> x.valid() }

  fun UserUpdate.valid(current: User) = ValidUserUpdate(
    username = username.getOrElse { current.username },
    email = email.getOrElse { current.email },
    encryptedPassword = password.map { auth.encryptPassword(it) },
    bio = bio.getOrElse { current.bio },
    image = image.getOrElse { current.image }
  )

  val validUpdate =
    { x: UserUpdate, current: User -> x.valid(current) }
}

fun User.registration() = UserRegistration(
  username = username,
  email = email,
  password = "plain"
)

fun User.update(password: Option<String> = none()) = UserUpdate(
  username = "$username.updated".some(),
  email = "$username.updated@realworld.io".some(),
  password = password,
  bio = bio.toOption(),
  image= image.toOption()
)

fun ValidUserRegistration.user() = User(
  id = id,
  email = email,
  token = token,
  username = username
)

fun ValidUserRegistration.userAndPassword() = UserAndPassword(
  user = user(),
  encryptedPassword = encryptedPassword
)
