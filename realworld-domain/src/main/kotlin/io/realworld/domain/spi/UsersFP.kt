package io.realworld.domain.spi

import arrow.core.Either
import arrow.effects.IO
import io.realworld.domain.api.UserRegistration
import io.realworld.domain.api.UserRegistrationValidationError

typealias ValidateUserRegistration =
  (reg: UserRegistration) -> IO<Either<UserRegistrationValidationError, UserRegistration>>
typealias SaveUser =
  (model: UserModel) -> IO<UserModel>

class UserNotFound
typealias GetUser = (email: String) -> IO<Either<UserNotFound, UserModel>>
