package io.realworld.profiles

import arrow.effects.ForIO
import arrow.effects.fix
import io.realworld.JwtTokenResolver
import io.realworld.authHeader
import io.realworld.domain.common.Auth
import io.realworld.domain.profiles.FollowCommand
import io.realworld.domain.profiles.FollowUseCase
import io.realworld.domain.profiles.GetProfileCommand
import io.realworld.domain.profiles.GetProfileUseCase
import io.realworld.domain.profiles.Profile
import io.realworld.domain.profiles.UnfollowCommand
import io.realworld.domain.profiles.UnfollowUseCase
import io.realworld.domain.users.User
import io.realworld.persistence.UserRepository
import io.realworld.runReadTx
import io.realworld.runWriteTx
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.NativeWebRequest

data class ProfileResponse(val profile: ProfileResponseDto) {
  companion object {
    fun fromDomain(domain: Profile) = ProfileResponse(ProfileResponseDto.fromDomain(domain))
  }
}

@RestController
class ProfileController(
  private val auth: Auth,
  private val repo: UserRepository<ForIO>,
  private val txManager: PlatformTransactionManager
) {
  @GetMapping("/api/profiles/{username}")
  fun getProfile(
    @PathVariable("username") username: String,
    webRequest: NativeWebRequest
  ): ResponseEntity<ProfileResponse> {

    val user = JwtTokenResolver(auth::parse)(
      webRequest.authHeader()
    ).toOption().flatMap {
      repo.findById(it.id).fix().unsafeRunSync().map { it.user }
    }

    return object : GetProfileUseCase<ForIO> {
      override val getUser = repo::findByUsername
      override val hasFollower = repo::hasFollower
      override val M = repo.MD
    }.run {
      GetProfileCommand(username, user).runUseCase()
    }.fix().runReadTx(txManager).fold(
      { ResponseEntity.notFound().build() },
      { ResponseEntity.ok(ProfileResponse.fromDomain(it)) }
    )
  }

  @PostMapping("/api/profiles/{username}/follow")
  fun follow(
    @PathVariable("username") username: String,
    current: User
  ): ResponseEntity<ProfileResponse> {
    return object : FollowUseCase<ForIO> {
      override val addFollower = repo::addFollower
      override val getUser = repo::findByUsername
      override val M = repo.MD
    }.run {
      FollowCommand(username, current).runUseCase()
    }.fix().runWriteTx(txManager).fold(
      { ResponseEntity.notFound().build() },
      { ResponseEntity.ok(ProfileResponse.fromDomain(it)) }
    )
  }

  @DeleteMapping("/api/profiles/{username}/follow")
  fun unfollow(
    @PathVariable("username") username: String,
    current: User
  ): ResponseEntity<ProfileResponse> {
    return object : UnfollowUseCase<ForIO> {
      override val getUser = repo::findByUsername
      override val removeFollower = repo::removeFollower
      override val M = repo.MD
    }.run {
      UnfollowCommand(username, current).runUseCase()
    }.fix().runWriteTx(txManager).fold(
      { ResponseEntity.notFound().build() },
      { ResponseEntity.ok(ProfileResponse.fromDomain(it)) }
    )
  }
}
