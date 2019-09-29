package io.realworld.domain.common

import arrow.core.Either
import arrow.core.left
import arrow.core.right

fun <L> Boolean.toEither(ifFalse: () -> L): Either<L, Unit> = if (this) Unit.right() else ifFalse().left()
