package io.realworld.domain.profiles

import arrow.core.Option
import arrow.effects.IO
import io.realworld.domain.users.User
import java.util.UUID

typealias GetUserByUsername = (username: String) -> IO<Option<User>>
typealias HasFollower = (followee: UUID, follower: UUID) -> IO<Boolean>
typealias AddFollower = (followee: UUID, follower: UUID) -> IO<Int>
typealias RemoveFollower = (followee: UUID, follower: UUID) -> IO<Int>
