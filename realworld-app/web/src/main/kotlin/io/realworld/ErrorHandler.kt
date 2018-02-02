package io.realworld

import com.fasterxml.jackson.annotation.JsonRootName
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
        handleError(HttpStatus.BAD_REQUEST, listOf(t.toValidationError()))
      }
      else -> handleError(HttpStatus.BAD_REQUEST, listOf(ValidationError(
        type = "ValidationError",
        path = "body",
        message = t.message ?: ""
      )))
    }
  }

  @ExceptionHandler(UnauthrorizedException::class)
  fun unauthorized() = handleError(HttpStatus.UNAUTHORIZED)

  private fun handleError(
    httpStatus: HttpStatus,
    validationErrors: List<ValidationError> = emptyList()
  ): ResponseEntity<ValidationErrorResponse> {
    val restErrors = ValidationErrorResponse(validationErrors.associateBy { it.path })
    return ResponseEntity(restErrors, httpStatus)
  }
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

@JsonRootName("errors")
class ValidationErrorResponse(m: Map<String, ValidationError>) : LinkedHashMap<String, ValidationError>(m) {}
