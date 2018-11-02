package io.realworld.domain.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.realworld.domain.users.UserId
import io.realworld.domain.users.userId
import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor
import java.util.UUID

inline class Token(val id: UserId)

class Auth(val settings: Settings.Security) {
  private val encryptor: PasswordEncryptor = StrongPasswordEncryptor()

  // TODO set expiration
  fun createToken(proto: Token) = Jwts.builder()
    .setSubject(proto.id.value.toString())
    .signWith(SignatureAlgorithm.HS512, settings.tokenSecret.toByteArray())
    .compact()

  fun parse(token: String): Token {
    val claims = Jwts.parser().setSigningKey(settings.tokenSecret.toByteArray()).parseClaimsJws(token)
    return Token(UUID.fromString(claims.body.subject).userId())
  }

  fun encryptPassword(plain: String) = encryptor.encryptPassword(plain)

  fun checkPassword(plain: String, encrypted: String) = encryptor.checkPassword(plain, encrypted)
}
