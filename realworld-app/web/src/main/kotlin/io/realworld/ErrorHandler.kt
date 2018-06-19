package io.realworld

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebInputException

@ControllerAdvice
class ErrorHandler {
  val log: Logger = LoggerFactory.getLogger(ErrorHandler::class.java)

  @ExceptionHandler(ServerWebInputException::class)
  fun serverWebInputException(ex: ServerWebInputException): ResponseEntity<ValidationErrorResponse> {
    val t = NestedExceptionUtils.getMostSpecificCause(ex)
    return when (t) {
      is MissingKotlinParameterException -> {
        handleError(HttpStatus.UNPROCESSABLE_ENTITY, listOf(t.toValidationError()))
      }
      else -> handleError(HttpStatus.UNPROCESSABLE_ENTITY, listOf(ValidationError(
        type = "ValidationError",
        path = "body",
        message = t.message ?: ""
      )))
    }
  }

  @ExceptionHandler(UnauthorizedException::class)
  fun unauthorized() = handleError(HttpStatus.UNAUTHORIZED)

  @ExceptionHandler(FieldError::class)
  fun fieldError(ex: FieldError) = handleError(HttpStatus.UNPROCESSABLE_ENTITY, listOf(ex.toValidationError()))
}

fun handleError(
  httpStatus: HttpStatus,
  validationErrors: List<ValidationError> = emptyList()
) = when (validationErrors.isEmpty()) {
  true -> ResponseEntity(httpStatus)
  else -> ResponseEntity(
    ValidationErrorResponse(validationErrors.associateBy { it.path }),
    httpStatus)
}

data class ValidationError(
  val type: String,
  val path: String,
  val message: String,
  val arguments: Map<String, Any>? = emptyMap()
)

fun JsonMappingException.toValidationErrorPath(): String =
  path.joinToString(separator = ".", transform = { it ->
    val i = if (it.index >= 0) "${it.index}" else ""
    "${it.fieldName ?: ""}${i}"
  })

fun MissingKotlinParameterException.toValidationError() = ValidationError(
  type = "TypeMismatch",
  path = toValidationErrorPath(),
  message = message ?: ""
)

class FieldError(val path: String, message: String) : Throwable(message) {
  fun toValidationError() = ValidationError(
    type = "FieldError",
    path = path,
    message = this.message ?: ""
  )
}

class ValidationErrorResponse(val errors: Map<String, ValidationError>)
