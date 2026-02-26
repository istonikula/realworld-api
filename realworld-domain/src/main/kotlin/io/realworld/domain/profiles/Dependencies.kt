package io.realworld.domain.profiles

import io.realworld.domain.users.User
import io.realworld.domain.users.UserId

typealias GetUserByUsername = suspend (username: String) -> User?
typealias HasFollower = suspend (followee: UserId, follower: UserId) -> Boolean
typealias AddFollower = suspend (followee: UserId, follower: UserId) -> Int
typealias RemoveFollower = suspend (followee: UserId, follower: UserId) -> Int
