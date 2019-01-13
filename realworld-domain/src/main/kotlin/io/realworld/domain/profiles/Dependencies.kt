package io.realworld.domain.profiles

import arrow.Kind
import arrow.core.Option
import io.realworld.domain.users.User
import io.realworld.domain.users.UserId

typealias GetUserByUsername<F> = (username: String) -> Kind<F, Option<User>>
typealias HasFollower<F> = (followee: UserId, follower: UserId) -> Kind<F, Boolean>
typealias AddFollower<F> = (followee: UserId, follower: UserId) -> Kind<F, Int>
typealias RemoveFollower<F> = (followee: UserId, follower: UserId) -> Kind<F, Int>
