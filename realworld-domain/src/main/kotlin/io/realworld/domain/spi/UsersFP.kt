package io.realworld.domain.spi

import arrow.core.Either
import io.realworld.domain.api.UserRegistration
import io.realworld.domain.api.UserRegistrationValidationError

typealias ValidateUserRegistration = (reg: UserRegistration) -> Either<UserRegistrationValidationError, UserRegistration>
