package io.realworld.persistence

interface BaseTbl {
  fun String.eq() = "$this = :$this"
  fun String.set() = "$this = :$this"
}

object UserTbl : BaseTbl {
  const val table = "users"
  const val id = "id"
  const val email = "email"
  const val token = "token"
  const val username = "username"
  const val password = "password"
  const val bio = "bio"
  const val image = "image"
}

object FollowTbl : BaseTbl {
  const val table = "follows"
  const val followee = "followee"
  const val follower = "follower"
}
