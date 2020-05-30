package io.realworld

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.realworld.errors.ErrorResponseDto
import io.realworld.errors.SingleErrorDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.util.WebUtils
import java.lang.Exception
import javax.servlet.ServletException

@ControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {
  // thrown when an object fails the @Valid validation
  override fun handleMethodArgumentNotValid(
    ex: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatus,
    request: WebRequest
  ): ResponseEntity<Any> {
    val fieldErrors = ex.bindingResult.fieldErrors.map<FieldError, SingleErrorDto>(FieldError::toErrorDto)
    val globalErrors = ex.bindingResult.globalErrors.map(ObjectError::toErrorDto)

    val httpStatus = HttpStatus.UNPROCESSABLE_ENTITY
    val servletWebRequest = request as ServletWebRequest

    val responseBody = ErrorResponseDto(
      status = httpStatus.value(),
      errorCode = "InvalidData",
      message = "There are validation errors in the data",
      path = servletWebRequest.request.servletPath,
      errors = listOf(fieldErrors, globalErrors).flatten()
    )
    return handleExceptionInternal(ex, responseBody, headers, httpStatus, request)
  }

  // use ErrorResponseDto also for errors detected by Spring
  override fun handleExceptionInternal(
    ex: Exception,
    body: Any?,
    headers: HttpHeaders,
    status: HttpStatus,
    request: WebRequest
  ): ResponseEntity<Any> {
    if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
      request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST)
    }

    val errorCode = ex::class.simpleName ?: "Undefined"
    val servletWebRequest = request as ServletWebRequest
    val responseBody = body ?: ErrorResponseDto(
      status = status.value(),
      errorCode = errorCode,
      message = ex.localizedMessage,
      path = servletWebRequest.request.servletPath,
      errors = listOf()
    )
    // TODO log error
    return ResponseEntity(responseBody, headers, status)
  }

  @ExceptionHandler(ForbiddenException::class)
  fun forbidden() = ResponseEntity<Unit>(HttpStatus.FORBIDDEN)

  @ExceptionHandler(UnauthorizedException::class)
  fun unauthorized() = ResponseEntity<Unit>(HttpStatus.UNAUTHORIZED)

  @ExceptionHandler(MyFieldError::class)
  fun fieldError(ex: MyFieldError) = handleError(HttpStatus.UNPROCESSABLE_ENTITY, listOf(ex.toValidationError()))

  // TODO find out why this overrides more specific errors
  // @ExceptionHandler(Throwable::class)
  fun handleAny(t: Throwable, request: WebRequest): ResponseEntity<ErrorResponseDto> {
    val cause = resolveCause(t)
    val msg = "Unexpected error"

    return ResponseEntity(
      ErrorResponseDto(
        status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
        errorCode = "Undefined",
        message = msg,
        path = (request as ServletWebRequest).request.servletPath,
        errors = listOf()
      ),
      HttpStatus.INTERNAL_SERVER_ERROR
    )
    // TODO log error
  }

  private fun resolveCause(t: Throwable): Throwable {
    var resolved = t
    while (resolved is ServletException && resolved.cause != null) {
      resolved = resolved.cause!!
    }
    return resolved
  }
}

fun FieldError.toErrorDto() = SingleErrorDto(
  message = defaultMessage ?: "Undefined error",
  metadata = mapOf("path" to field)
)

fun ObjectError.toErrorDto() = SingleErrorDto(
  message = defaultMessage ?: "Undefined error",
  metadata = mapOf("path" to objectName)
)

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

class MyFieldError(val path: String, message: String) : Throwable(message) {
  fun toValidationError() = ValidationError(
    type = "FieldError",
    path = path,
    message = this.message ?: ""
  )
}

class ValidationErrorResponse(val errors: Map<String, ValidationError>)
