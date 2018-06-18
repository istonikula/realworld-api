package io.realworld.domain.users

import arrow.core.Either
import arrow.core.Option
import arrow.effects.IO
import java.util.*

typealias ValidateUserRegistration = (reg: UserRegistration) -> IO<Either<UserRegistrationError, ValidUserRegistration>>
typealias CreateUser = (user: ValidUserRegistration) -> IO<User>

typealias ValidateUserUpdate = (update: UserUpdate, current: User) -> IO<Either<UserUpdateError, ValidUserUpdate>>
typealias UpdateUser = (update: ValidUserUpdate, current: User) -> IO<User>

typealias GetUserByEmail = (email: String) -> IO<Option<UserAndPassword>>

interface UserRepository {
  fun create(user: ValidUserRegistration): IO<User>
  fun update(update: ValidUserUpdate, current: User): IO<User>
  fun findById(id: UUID): IO<Option<UserAndPassword>>
  fun findByEmail(email: String): IO<Option<UserAndPassword>>
  fun findByUsername(username: String): IO<Option<User>>
  fun existsByEmail(email: String): IO<Boolean>
  fun existsByUsername(username: String): IO<Boolean>
  fun hasFollower(followeeUsername: String, followerUsername: String): IO<Boolean>
  fun addFollower(followeeUsername: String, followerUsername: String): IO<Int>
}
