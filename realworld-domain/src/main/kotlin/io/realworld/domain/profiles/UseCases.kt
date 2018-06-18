package io.realworld.domain.profiles

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import arrow.core.toOption
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.extensions
import arrow.effects.fix
import arrow.typeclasses.binding
import io.realworld.domain.users.User


data class GetProfileCommand(val username: String, val current: Option<User>)

interface GetProfileUseCase {
  val getUser: GetUserByUsername
  val hasFollower: HasFollower

  fun GetProfileCommand.runUseCase(): IO<Option<Profile>> {
    val cmd = this
    return ForIO extensions {
      binding {
        getUser(cmd.username).bind().fold(
          { none<Profile>() },
          {
            Profile(
              username = it.username,
              bio = it.bio.toOption(),
              image = it.image.toOption(),
              following = current.fold(
                { none<Boolean>() },
                { hasFollower(it.username, cmd.username).bind().some() }
              )
            ).some()
          }
        )
      }.fix()
    }
  }
}
