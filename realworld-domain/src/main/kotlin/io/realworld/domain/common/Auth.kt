package io.realworld.domain.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor

data class Token(val email: String)

class Auth(val settings: Settings.Security){
  private val encryptor: PasswordEncryptor = StrongPasswordEncryptor()

  // TODO set expiration
  fun createToken(proto: Token) = Jwts.builder()
    .setSubject(proto.email)
    .signWith(SignatureAlgorithm.HS512, settings.tokenSecret.toByteArray())
    .compact()

  fun parse(token: String): Token {
    val claims = Jwts.parser().setSigningKey(settings.tokenSecret.toByteArray()).parseClaimsJws(token)
    return Token(claims.body.subject)
  }

  fun encryptPassword(plain: String) = encryptor.encryptPassword(plain)

  fun checkPassword(plain: String, encrypted: String) = encryptor.checkPassword(plain, encrypted)
}
