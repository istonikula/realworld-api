package io.realworld.domain.users

import arrow.core.Either
import arrow.core.Option
import arrow.effects.IO

typealias ValidateUserRegistration = (reg: UserRegistration) -> IO<Either<UserRegistrationError, UserRegistration>>
typealias CreateUser = (user: ValidUserRegistration) -> IO<User>

typealias ValidateUserUpdate = (update: UserUpdate) -> IO<Either<UserUpdateError, UserUpdate>>
typealias UpdateUser = (update: ValidUserUpdate, current: User) -> IO<User>

typealias GetUserByEmail = (email: String) -> IO<Option<UserAndPassword>>

interface UserRepository {
  fun create(user: ValidUserRegistration): IO<User>
  fun update(update: ValidUserUpdate, current: User): IO<User>
  fun findByEmail(email: String): IO<Option<UserAndPassword>>
  fun existsByEmail(email: String): IO<Boolean>
  fun existsByUsername(username: String): IO<Boolean>
}
