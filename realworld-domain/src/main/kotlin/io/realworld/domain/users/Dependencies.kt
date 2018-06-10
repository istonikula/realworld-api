package io.realworld.domain.users

import arrow.core.Either
import arrow.effects.IO

typealias ValidateUserRegistration = (reg: UserRegistration) -> IO<Either<UserRegistrationError, UserRegistration>>
typealias CreateUser = (user: ValidUserRegistration) -> IO<User>

typealias ValidateUserUpdate = (update: UserUpdate) -> IO<Either<UserUpdateError, UserUpdate>>
typealias UpdateUser = (update: ValidUserUpdate, current: User) -> IO<User>

typealias GetUser = (email: String) -> IO<Either<UserNotFound, UserAndPassword>>

interface UserRepository {
  fun create(user: ValidUserRegistration): IO<User>
  fun update(update: ValidUserUpdate, current: User): IO<User>
  fun findByEmail(email: String): UserAndPassword?
  fun existsByEmail(email: String): Boolean
  fun existsByUsername(username: String): Boolean
}
