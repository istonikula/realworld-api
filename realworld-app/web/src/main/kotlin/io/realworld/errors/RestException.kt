package io.realworld.errors

import io.realworld.domain.common.DomainError
import org.springframework.http.HttpStatus
import io.realworld.domain.articles.ArticleDeleteError
import io.realworld.domain.articles.ArticleFavoriteError
import io.realworld.domain.articles.ArticleUpdateError
import io.realworld.domain.users.UserLoginError
import io.realworld.domain.users.UserRegistrationError
import io.realworld.domain.users.UserUpdateError

sealed class RestException(val status: HttpStatus) : RuntimeException() {
  abstract val error: DomainError
  data class BadRequest(override val error: DomainError) : RestException(HttpStatus.BAD_REQUEST)
  data class Conflict(override val error: DomainError) : RestException(HttpStatus.CONFLICT)
  data class Forbidden(override val error: DomainError) : RestException(HttpStatus.FORBIDDEN)
  data class NotFound(override val error: DomainError) : RestException(HttpStatus.NOT_FOUND)
  data class Unauthorized(override val error: DomainError) : RestException(HttpStatus.UNAUTHORIZED)
  data class UnprocessableEntity(override val error: DomainError) : RestException(HttpStatus.UNPROCESSABLE_ENTITY)
  data class InternalServerError(override val error: DomainError) : RestException(HttpStatus.INTERNAL_SERVER_ERROR)
}

fun UserLoginError.toRestException(): Nothing = when (this) {
  is UserLoginError.BadCredentials ->
    throw RestException.Unauthorized(this)
}

fun UserRegistrationError.toRestException(): Nothing = when (this) {
  is UserRegistrationError.EmailAlreadyTaken ->
    throw RestException.Conflict(this)
  is UserRegistrationError.UsernameAlreadyTaken ->
    throw RestException.Conflict(this)
}

fun UserUpdateError.toRestException(): Nothing = when (this) {
  is UserUpdateError.EmailAlreadyTaken ->
    throw RestException.Conflict(this)
  is UserUpdateError.UsernameAlreadyTaken ->
    throw RestException.Conflict(this)
}

fun ArticleDeleteError.toRestException(): Nothing = when (this) {
  is ArticleDeleteError.NotAuthor ->
    throw RestException.Forbidden(this)
  is ArticleDeleteError.NotFound ->
    throw RestException.NotFound(this)
}

fun ArticleFavoriteError.toRestException(): Nothing = when (this) {
  is ArticleFavoriteError.Author ->
    throw RestException.Forbidden(this)
  is ArticleFavoriteError.NotFound ->
    throw RestException.NotFound(this)
}

fun ArticleUpdateError.toRestException(): Nothing = when (this) {
  is ArticleUpdateError.NotAuthor ->
    throw RestException.Forbidden(this)
  is ArticleUpdateError.NotFound ->
    throw RestException.NotFound(this)
}
