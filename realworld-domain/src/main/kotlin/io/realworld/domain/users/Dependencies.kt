package io.realworld.domain.users

import arrow.core.Either
import arrow.core.Option
import arrow.fx.IO

typealias ValidateUserRegistration = (reg: UserRegistration) -> IO<Either<UserRegistrationError, ValidUserRegistration>>
typealias CreateUser = (user: ValidUserRegistration) -> IO<User>

typealias ValidateUserUpdate = (update: UserUpdate, current: User) -> IO<Either<UserUpdateError, ValidUserUpdate>>
typealias UpdateUser = (update: ValidUserUpdate, current: User) -> IO<User>

typealias GetUserByEmail = (email: String) -> IO<Option<UserAndPassword>>

typealias ExistsByEmail = (email: String) -> IO<Boolean>
typealias ExistsByUsername = (username: String) -> IO<Boolean>
