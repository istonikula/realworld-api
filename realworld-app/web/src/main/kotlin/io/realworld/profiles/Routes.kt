package io.realworld.profiles

import io.realworld.JwtTokenResolver
import io.realworld.domain.common.Auth
import io.realworld.domain.profiles.GetProfileCommand
import io.realworld.domain.profiles.GetProfileUseCase
import io.realworld.domain.profiles.Profile
import io.realworld.domain.users.UserRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

data class ProfileResponse(val profile: ProfileResponseDto) {
  companion object {
    fun fromDomain(domain: Profile) = ProfileResponse(ProfileResponseDto.fromDomain(domain))
  }
}

@RestController
class ProfileController(
  private val auth: Auth,
  private val repo: UserRepository

) {

  @GetMapping("/api/profiles/{username}")
  fun getProfile(
    @PathVariable("username") username: String,
    exchange: ServerWebExchange
  ): ResponseEntity<ProfileResponse> {

    val user = JwtTokenResolver(auth::parse)(exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)).toOption().flatMap {
      repo.findById(it.id).unsafeRunSync().map { it.user }
    }

    return object : GetProfileUseCase {
      override val hasFollower = repo::hasFollower
      override val getUser = repo::findByUsername
    }.run {
      GetProfileCommand(username, user).runUseCase()
    }.unsafeRunSync().fold(
      { ResponseEntity.notFound().build() },
      { ResponseEntity.ok(ProfileResponse.fromDomain(it)) }
    )
  }

}
