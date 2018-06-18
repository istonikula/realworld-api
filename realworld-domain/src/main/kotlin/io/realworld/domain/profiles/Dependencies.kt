package io.realworld.domain.profiles

import arrow.core.Option
import arrow.effects.IO
import io.realworld.domain.users.User

typealias GetUserByUsername = (username: String) -> IO<Option<User>>
typealias HasFollower = (followeeUsername: String, followerUsername: String) -> IO<Boolean>
