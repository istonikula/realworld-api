package io.realworld.errors

import java.time.Instant

data class SingleErrorDto(val message: String, val metadata: Any? = null)

data class ErrorResponseDto(
  val status: Int,
  val errorCode: String,
  val message: String,
  val path: String,
  val timestamp: Instant = Instant.now(),
  val errors: List<SingleErrorDto>
)
