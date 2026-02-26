package io.realworld.domain.fixtures

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
  fun createUser(username: String, id: UserId = UUID.randomUUID().userId()) = User(
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
    username = username ?: current.username,
    email = email ?: current.email,
    encryptedPassword = password?.let { auth.encryptPassword(it) },
    bio = bio ?: current.bio,
    image = image ?: current.image
  )

  val validUpdate =
    { x: UserUpdate, current: User -> x.valid(current) }
}

fun User.registration() = UserRegistration(
  username = username,
  email = email,
  password = "plain"
)

fun User.update(password: String? = null) = UserUpdate(
  username = "$username.updated",
  email = "$username.updated@realworld.io",
  password = password,
  bio = bio,
  image = image
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
