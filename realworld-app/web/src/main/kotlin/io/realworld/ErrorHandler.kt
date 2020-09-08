package io.realworld

import io.realworld.domain.common.DomainError
import io.realworld.errors.ErrorResponseDto
import io.realworld.errors.RestException
import io.realworld.errors.SingleErrorDto
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.util.WebUtils
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

  @ExceptionHandler(RestException::class)
  fun handleRestException(e: RestException, req: WebRequest) = e.error.toErrorResponse(e.status, req)

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

fun DomainError.toErrorResponse(
  status: HttpStatus,
  request: WebRequest
): ResponseEntity<ErrorResponseDto> = when (this) {
  is DomainError.Single -> toErrorResponse(status, request)
  is DomainError.Multi -> toErrorResponse(status, request)
}

fun DomainError.Single.toErrorResponse(
  status: HttpStatus,
  request: WebRequest
): ResponseEntity<ErrorResponseDto> = ResponseEntity(
  ErrorResponseDto(
    status = status.value(),
    errorCode = this::class.simpleName ?: "Undefined",
    message = msg,
    path = (request as ServletWebRequest).request.servletPath,
    errors = listOf()
  ),
  status
)

fun DomainError.Multi.toErrorResponse(
  status: HttpStatus,
  request: WebRequest
): ResponseEntity<ErrorResponseDto> = ResponseEntity(
  ErrorResponseDto(
    status = status.value(),
    errorCode = errorCode,
    message = msg,
    path = (request as ServletWebRequest).request.servletPath,
    errors = errors.all.map { SingleErrorDto(it.msg) }
  ),
  status
)
