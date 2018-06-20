package io.realworld

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.realworld.JwtError.NoToken
import io.realworld.JwtError.ParseFail
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.NativeWebRequest

typealias ResolveToken<T> = (authHeader: String?) -> Either<JwtError, T>
typealias ParseToken<T> = (token: String) -> T


sealed class JwtError {
  object ParseFail : JwtError()
  object NoToken : JwtError()
}

class JwtTokenResolver<T>(val parseToken: ParseToken<T>) : ResolveToken<T> {
  override fun invoke(authHeader: String?): Either<JwtError, T> {
    authHeader?.apply {
      if (startsWith(TOKEN_PREFIX)) {
        return try {
          parseToken(substring(TOKEN_PREFIX.length)).right()
        } catch (t: Throwable) {
          ParseFail.left()
        }
      }
    }
    return NoToken.left()
  }

  companion object {
    val TOKEN_PREFIX = "Token "
  }
}

fun NativeWebRequest.authHeader() = this.getHeader(HttpHeaders.AUTHORIZATION)
