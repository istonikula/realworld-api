package io.realworld.domain.users

import arrow.Kind
import arrow.core.Either
import arrow.core.Option

typealias ValidateUserRegistration<F> =
  (reg: UserRegistration) -> Kind<F, Either<UserRegistrationError, ValidUserRegistration>>
typealias CreateUser<F> = (user: ValidUserRegistration) -> Kind<F, User>

typealias ValidateUserUpdate<F> =
  (update: UserUpdate, current: User) -> Kind<F, Either<UserUpdateError, ValidUserUpdate>>
typealias UpdateUser<F> = (update: ValidUserUpdate, current: User) -> Kind<F, User>

typealias GetUserByEmail<F> = (email: String) -> Kind<F, Option<UserAndPassword>>

typealias ExistsByEmail<F> = (email: String) -> Kind<F, Boolean>
typealias ExistsByUsername<F> = (username: String) -> Kind<F, Boolean>
