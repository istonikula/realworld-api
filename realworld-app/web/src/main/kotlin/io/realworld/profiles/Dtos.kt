// ktlint-disable filename
package io.realworld.profiles

import io.realworld.domain.profiles.Profile

data class ProfileResponseDto(
  val username: String,
  val bio: String? = null,
  val image: String? = null,
  val following: Boolean? = null
) {
  companion object {
    fun fromDomain(domain: Profile) = with(domain) {
      ProfileResponseDto(
        username = username,
        bio = bio.getOrNull(),
        image = image.getOrNull(),
        following = following.getOrNull()
      )
    }
  }
}
