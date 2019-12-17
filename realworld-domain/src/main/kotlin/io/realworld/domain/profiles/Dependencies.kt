package io.realworld.domain.profiles

import arrow.core.Option
import arrow.fx.IO
import io.realworld.domain.users.User
import io.realworld.domain.users.UserId

typealias GetUserByUsername = (username: String) -> IO<Option<User>>
typealias HasFollower = (followee: UserId, follower: UserId) -> IO<Boolean>
typealias AddFollower = (followee: UserId, follower: UserId) -> IO<Int>
typealias RemoveFollower = (followee: UserId, follower: UserId) -> IO<Int>
