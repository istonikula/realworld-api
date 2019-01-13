package io.realworld.domain.profiles

import arrow.Kind
import arrow.core.Option
import arrow.core.none
import arrow.core.some
import arrow.core.toOption
import arrow.typeclasses.Monad
import arrow.typeclasses.binding
import io.realworld.domain.users.User

data class GetProfileCommand(val username: String, val current: Option<User>)
data class FollowCommand(val username: String, val current: User)
data class UnfollowCommand(val username: String, val current: User)

interface GetProfileUseCase<F> {
  val getUser: GetUserByUsername<F>
  val hasFollower: HasFollower<F>
  val M: Monad<F>

  fun GetProfileCommand.runUseCase(): Kind<F, Option<Profile>> {
    val cmd = this
    return M.binding {
      getUser(cmd.username).bind().fold(
        { none<Profile>() },
        {
          Profile(
            username = it.username,
            bio = it.bio.toOption(),
            image = it.image.toOption(),
            following = current.fold(
              { none<Boolean>() },
              { follower -> hasFollower(it.id, follower.id).bind().some() }
            )
          ).some()
        }
      )
    }
  }
}

interface FollowUseCase<F> {
  val getUser: GetUserByUsername<F>
  val addFollower: AddFollower<F>
  val M: Monad<F>

  fun FollowCommand.runUseCase(): Kind<F, Option<Profile>> {
    val cmd = this
    return M.binding {
      getUser(cmd.username).bind().fold(
        { none<Profile>() },
        {
          addFollower(it.id, cmd.current.id).bind()
          Profile(
            username = it.username,
            bio = it.bio.toOption(),
            image = it.image.toOption(),
            following = true.some()
          ).some()
        }
      )
    }
  }
}

interface UnfollowUseCase<F> {
  val getUser: GetUserByUsername<F>
  val removeFollower: RemoveFollower<F>
  val M: Monad<F>

  fun UnfollowCommand.runUseCase(): Kind<F, Option<Profile>> {
    val cmd = this
    return M.binding {
      getUser(cmd.username).bind().fold(
        { none<Profile>() },
        {
          removeFollower(it.id, cmd.current.id).bind()
          Profile(
            username = it.username,
            bio = it.bio.toOption(),
            image = it.image.toOption(),
            following = false.some()
          ).some()
        }
      )
    }
  }
}
