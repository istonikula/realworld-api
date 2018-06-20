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
        bio = bio.orNull(),
        image = image.orNull(),
        following = following.orNull()
      )
    }
  }
}

private interface KtlintDisableFilenameRule
