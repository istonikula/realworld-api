package io.realworld.domain.users

import arrow.core.Either
import arrow.effects.IO

typealias ValidateUserRegistration =
  (reg: UserRegistration) -> IO<Either<UserRegistrationValidationError, UserRegistration>>
typealias ValidateUserUpdate =
  (upd: UserUpdate) -> IO<Either<UserUpdateValidationError, UserUpdate>>

typealias SaveUser = (user: User) -> IO<User>

typealias CreateUser = (user: ValidUserRegistration) -> IO<User>

class UserNotFound
typealias GetUser = (email: String) -> IO<Either<UserNotFound, UserAndPassword>>

interface UserRepository {
  fun create(user: ValidUserRegistration): User
  fun update(user: User): User
  fun findByEmail(email: String): UserAndPassword?
  fun existsByEmail(email: String): Boolean
  fun existsByUsername(username: String): Boolean
}
