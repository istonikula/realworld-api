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

object ArticleTbl : BaseTbl {
  const val table = "articles"
  const val id = "id"
  const val slug = "slug"
  const val title = "title"
  const val description = "description"
  const val body = "body"
  const val author = "author"
  const val created_at = "created_at"
  const val updated_at = "updated_at"
}

object TagTbl : BaseTbl {
  const val table = "tags"
  const val name = "name"
}

object ArticleTagTbl : BaseTbl {
  const val table = "article_tags"
  const val article_id = "article_id"
  const val tag = "tag"
}
