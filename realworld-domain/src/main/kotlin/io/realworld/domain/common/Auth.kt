package io.realworld.domain.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor
import java.util.*

data class Token(val id: UUID)

class Auth(val settings: Settings.Security){
  private val encryptor: PasswordEncryptor = StrongPasswordEncryptor()

  // TODO set expiration
  fun createToken(proto: Token) = Jwts.builder()
    .setSubject(proto.id.toString())
    .signWith(SignatureAlgorithm.HS512, settings.tokenSecret.toByteArray())
    .compact()

  fun parse(token: String): Token {
    val claims = Jwts.parser().setSigningKey(settings.tokenSecret.toByteArray()).parseClaimsJws(token)
    return Token(UUID.fromString(claims.body.subject))
  }

  fun encryptPassword(plain: String) = encryptor.encryptPassword(plain)

  fun checkPassword(plain: String, encrypted: String) = encryptor.checkPassword(plain, encrypted)
}
