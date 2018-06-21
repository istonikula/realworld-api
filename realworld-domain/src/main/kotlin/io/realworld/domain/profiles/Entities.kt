package io.realworld.domain.profiles

import arrow.core.Option

data class Profile(
  val username: String,
  val bio: Option<String>,
  val image: Option<String>,
  val following: Option<Boolean>
) { companion object }

private interface ktlintDisableFilenameRule
