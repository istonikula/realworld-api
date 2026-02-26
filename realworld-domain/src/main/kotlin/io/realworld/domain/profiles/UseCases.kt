package io.realworld.domain.profiles

import io.realworld.domain.users.User

data class GetProfileCommand(val username: String, val current: User?)
data class FollowCommand(val username: String, val current: User)
data class UnfollowCommand(val username: String, val current: User)

interface GetProfileUseCase {
  val getUser: GetUserByUsername
  val hasFollower: HasFollower

  suspend fun GetProfileCommand.runUseCase(): Profile? {
    val cmd = this

    return getUser(cmd.username)?.let {
      Profile(
        username = it.username,
        bio = it.bio,
        image = it.image,
        following = cmd.current?.let { follower -> hasFollower(it.id, follower.id) }
      )
    }
  }
}

interface FollowUseCase {
  val getUser: GetUserByUsername
  val addFollower: AddFollower

  suspend fun FollowCommand.runUseCase(): Profile? {
    val cmd = this

    return getUser(cmd.username)?.let {
      addFollower(it.id, cmd.current.id)
      Profile(
        username = it.username,
        bio = it.bio,
        image = it.image,
        following = true
      )
    }
  }
}

interface UnfollowUseCase {
  val getUser: GetUserByUsername
  val removeFollower: RemoveFollower

  suspend fun UnfollowCommand.runUseCase(): Profile? {
    val cmd = this

    return getUser(cmd.username)?.let {
      Profile(
        username = it.username,
        bio = it.bio,
        image = it.image,
        following = false
      )
    }
  }
}
