package io.realworld.domain.spi

import arrow.core.Either
import arrow.effects.IO
import io.realworld.domain.api.UserRegistration
import io.realworld.domain.api.UserRegistrationValidationError
import io.realworld.domain.api.UserUpdate
import io.realworld.domain.api.UserUpdateValidationError

typealias ValidateUserRegistration =
  (reg: UserRegistration) -> IO<Either<UserRegistrationValidationError, UserRegistration>>
typealias ValidateUserUpdate =
  (upd: UserUpdate) -> IO<Either<UserUpdateValidationError, UserUpdate>>

typealias SaveUser =
  (model: UserModel) -> IO<UserModel>

class UserNotFound
typealias GetUser = (email: String) -> IO<Either<UserNotFound, UserModel>>
