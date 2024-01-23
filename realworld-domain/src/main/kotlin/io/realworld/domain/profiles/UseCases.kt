package io.realworld.domain.profiles

import arrow.core.Option
import arrow.core.some
import arrow.core.toOption
import io.realworld.domain.users.User

data class GetProfileCommand(val username: String, val current: Option<User>)
data class FollowCommand(val username: String, val current: User)
data class UnfollowCommand(val username: String, val current: User)

interface GetProfileUseCase {
  val getUser: GetUserByUsername
  val hasFollower: HasFollower

  suspend fun GetProfileCommand.runUseCase(): Option<Profile> {
    val cmd = this

    return getUser(cmd.username).map {
      Profile(
        username = it.username,
        bio = it.bio.toOption(),
        image = it.image.toOption(),
        following = cmd.current.map { follower -> hasFollower(it.id, follower.id) }
      )
    }
  }
}

interface FollowUseCase {
  val getUser: GetUserByUsername
  val addFollower: AddFollower

  suspend fun FollowCommand.runUseCase(): Option<Profile> {
    val cmd = this

    return getUser(cmd.username).map {
      addFollower(it.id, cmd.current.id)
      Profile(
        username = it.username,
        bio = it.bio.toOption(),
        image = it.image.toOption(),
        following = true.some()
      )
    }
  }
}

interface UnfollowUseCase {
  val getUser: GetUserByUsername
  val removeFollower: RemoveFollower

  suspend fun UnfollowCommand.runUseCase(): Option<Profile> {
    val cmd = this

    return getUser(cmd.username).map {
      Profile(
        username = it.username,
        bio = it.bio.toOption(),
        image = it.image.toOption(),
        following = false.some()
      )
    }
  }
}
