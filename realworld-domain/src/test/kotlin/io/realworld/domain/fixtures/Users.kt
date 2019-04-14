package io.realworld.domain.fixtures

import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import io.realworld.domain.users.User
import io.realworld.domain.users.UserAndPassword
import io.realworld.domain.users.UserId
import io.realworld.domain.users.UserRegistration
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.domain.users.userId
import java.util.UUID

class UserFactory(val auth: Auth) {
  fun createRegistration(username: String) = UserRegistration(
    username = username,
    email = "$username@realworld.io",
    password = "plain"
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
}

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


