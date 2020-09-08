package io.realworld

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.toOption
import io.realworld.domain.common.AuthError
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.NativeWebRequest

typealias ResolveToken<T> = (authHeader: String?) -> Either<AuthError, T>
typealias ParseToken<T> = (token: String) -> Either<AuthError, T>

class JwtTokenResolver<T>(val parseToken: ParseToken<T>) : ResolveToken<T> {
  override fun invoke(authHeader: String?): Either<AuthError, T> =
    authHeader.toOption()
      .filter { it.startsWith(TOKEN_PREFIX) }
      .toEither { AuthError.InvalidAuthorizationHeader }
      .flatMap { parseToken(it.substring(TOKEN_PREFIX.length)) }

  companion object {
    const val TOKEN_PREFIX = "Token "
  }
}

fun NativeWebRequest.authHeader() = this.getHeader(HttpHeaders.AUTHORIZATION)
