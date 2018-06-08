package io.realworld.domain.users

import arrow.core.Either
import arrow.effects.IO

typealias ValidateUserRegistration =
  (reg: UserRegistration) -> IO<Either<UserRegistrationValidationError, UserRegistration>>
typealias ValidateUserUpdate =
  (upd: UserUpdate) -> IO<Either<UserUpdateValidationError, UserUpdate>>

typealias SaveUser =
  (model: UserModel) -> IO<UserModel>

class UserNotFound
typealias GetUser = (email: String) -> IO<Either<UserNotFound, UserModel>>

interface UserRepository {
  fun save(user: UserModel): UserModel
  fun findByEmail(email: String): UserModel?
  fun existsByEmail(email: String): Boolean
  fun existsByUsername(username: String): Boolean
}
