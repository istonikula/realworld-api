package io.realworld

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.NativeWebRequest

typealias ResolveToken<T> = (authHeader: String?) -> Either<AuthError, T>
typealias ParseToken<T> = (token: String) -> T

class JwtTokenResolver<T>(val parseToken: ParseToken<T>) : ResolveToken<T> {
  override fun invoke(authHeader: String?): Either<AuthError, T> {
    authHeader?.apply {
      if (startsWith(TOKEN_PREFIX)) {
        return try {
          parseToken(substring(TOKEN_PREFIX.length)).right()
        } catch (t: Throwable) {
          AuthError.InvalidToken.left()
        }
      }
    }
    return AuthError.InvalidAuthorizationHeader.left()
  }

  companion object {
    const val TOKEN_PREFIX = "Token "
  }
}

fun NativeWebRequest.authHeader() = this.getHeader(HttpHeaders.AUTHORIZATION)
