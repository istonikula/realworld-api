// ktlint-disable filename
package io.realworld.domain.common

import arrow.core.Nel

sealed class DomainError {
  abstract class Single() : DomainError() {
    abstract val msg: String
  }

  data class Multi(
    val errors: Nel<Single>,
    val msg: String = "Multiple errors encountered",
    val errorCode: String = "MultiError"
  ) : DomainError()
}
