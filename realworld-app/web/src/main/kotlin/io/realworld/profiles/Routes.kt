package io.realworld.profiles

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
  private val repo: UserRepository,
  private val txManager: PlatformTransactionManager

) {
  @GetMapping("/api/profiles/{username}")
  fun getProfile(
    @PathVariable("username") username: String,
    webRequest: NativeWebRequest
  ): ResponseEntity<ProfileResponse> {
    return runReadTx(txManager) {
      val user = JwtTokenResolver(auth::parse)(
        webRequest.authHeader()
      ).getOrNone().flatMap { token ->
        repo.findById(token.id).map { it.user }
      }
      val getUser = repo::findByUsername
      val hasFollower = repo::hasFollower
      object : GetProfileUseCase {
        override val getUser = getUser
        override val hasFollower = hasFollower
      }.run {
        GetProfileCommand(username, user).runUseCase()
      }.fold(
        { ResponseEntity.notFound().build() },
        { ResponseEntity.ok(ProfileResponse.fromDomain(it)) }
      )
    }
  }

  @PostMapping("/api/profiles/{username}/follow")
  fun follow(
    @PathVariable("username") username: String,
    current: User
  ): ResponseEntity<ProfileResponse> {
    return runWriteTx(txManager) {
      val addFollower = repo::addFollower
      val getUser = repo::findByUsername
      object : FollowUseCase {
        override val addFollower = addFollower
        override val getUser = getUser
      }.run {
        FollowCommand(username, current).runUseCase()
      }.fold(
        { ResponseEntity.notFound().build() },
        { ResponseEntity.ok(ProfileResponse.fromDomain(it)) }
      )
    }
  }

  @DeleteMapping("/api/profiles/{username}/follow")
  fun unfollow(
    @PathVariable("username") username: String,
    current: User
  ): ResponseEntity<ProfileResponse> {
    return runWriteTx(txManager) {
      val getUser = repo::findByUsername
      val removeFollower = repo::removeFollower
      object : UnfollowUseCase {
        override val getUser = getUser
        override val removeFollower = removeFollower
      }.run {
        UnfollowCommand(username, current).runUseCase()
      }.fold(
        { ResponseEntity.notFound().build() },
        { ResponseEntity.ok(ProfileResponse.fromDomain(it)) }
      )
    }
  }
}
