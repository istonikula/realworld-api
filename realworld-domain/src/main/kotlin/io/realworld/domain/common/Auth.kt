package io.realworld.domain.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.realworld.domain.users.UserId
import io.realworld.domain.users.userId
import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor
import java.util.UUID

@JvmInline value class Token(val id: UserId)

class Auth(val settings: Settings.Security) {
  private val encryptor: PasswordEncryptor = StrongPasswordEncryptor()
  private val key = Keys.hmacShaKeyFor(settings.tokenSecret.toByteArray())

  // TODO set expiration
  fun createToken(proto: Token) = Jwts.builder()
    .subject(proto.id.value.toString())
    .signWith(key, Jwts.SIG.HS512)
    .compact()

  fun parse(token: String): Token {
    val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
    return Token(UUID.fromString(claims.body.subject).userId())
  }

  fun encryptPassword(plain: String) = encryptor.encryptPassword(plain)

  fun checkPassword(plain: String, encrypted: String) = encryptor.checkPassword(plain, encrypted)
}
