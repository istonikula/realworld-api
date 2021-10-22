package io.realworld.domain.users

import arrow.core.Either
import arrow.core.Option

typealias ValidateUserRegistration = suspend (reg: UserRegistration) ->
  Either<UserRegistrationError, ValidUserRegistration>
typealias CreateUser = suspend (user: ValidUserRegistration) -> User

typealias ValidateUserUpdate = suspend (update: UserUpdate, current: User) -> Either<UserUpdateError, ValidUserUpdate>
typealias UpdateUser = suspend (update: ValidUserUpdate, current: User) -> User

typealias GetUserByEmail = suspend (email: String) -> Option<UserAndPassword>

typealias ExistsByEmail = suspend (email: String) -> Boolean
typealias ExistsByUsername = suspend (username: String) -> Boolean
