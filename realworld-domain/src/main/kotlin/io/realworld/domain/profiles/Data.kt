// ktlint-disable filename
package io.realworld.domain.profiles

data class Profile(
  val username: String,
  val bio: String?,
  val image: String?,
  val following: Boolean?
) { companion object }
