package io.realworld.domain.common

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.realworld.domain.users.UserId
import io.realworld.domain.users.userId
import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor
import java.util.UUID

inline class Token(val id: UserId)

sealed class AuthError(override val msg: String) : DomainError.Single() {
  data class InvalidToken(val cause: Throwable) : AuthError("Invalid token")
  object InvalidAuthorizationHeader : AuthError("Invalid authorization header")
  object BadCredentials : AuthError("Bad credentials")
}

class Auth(val settings: Settings.Security) {
  private val encryptor: PasswordEncryptor = StrongPasswordEncryptor()

  // TODO set expiration
  fun createToken(proto: Token): String = Jwts.builder()
    .setSubject(proto.id.value.toString())
    .signWith(SignatureAlgorithm.HS512, settings.tokenSecret.toByteArray())
    .compact()

  fun parse(token: String): Either<AuthError, Token> =
    parseClaims(token).map { Token(UUID.fromString(it.body.subject).userId()) }

  fun encryptPassword(plain: String): String = encryptor.encryptPassword(plain)

  fun checkPassword(plain: String, encrypted: String) = encryptor.checkPassword(plain, encrypted)

  private fun parseClaims(token: String): Either<AuthError, Jws<Claims>> =
    try {
      Jwts.parser().setSigningKey(settings.tokenSecret.toByteArray()).parseClaimsJws(token).right()
    } catch (t: Throwable) {
      AuthError.InvalidToken(t).left()
    }
}
