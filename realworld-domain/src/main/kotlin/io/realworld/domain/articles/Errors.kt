package io.realworld.domain.articles

import io.realworld.domain.common.DomainError

object ErrorMsg {
  const val notFound = "Not found"
}

sealed class ArticleUpdateError(override val msg: String) : DomainError.Single() {
  object NotAuthor : ArticleUpdateError("Not author")
  object NotFound : ArticleUpdateError(ErrorMsg.notFound)
}

sealed class ArticleDeleteError(override val msg: String) : DomainError.Single() {
  object NotAuthor : ArticleDeleteError("Not author")
  object NotFound : ArticleDeleteError(ErrorMsg.notFound)
}

sealed class ArticleFavoriteError(override val msg: String) : DomainError.Single() {
  object Author : ArticleFavoriteError("Author cannot favorite")
  object NotFound : ArticleFavoriteError(ErrorMsg.notFound)
}

sealed class ArticleUnfavoriteError(override val msg: String) : DomainError.Single() {
  object NotFound : ArticleUnfavoriteError(ErrorMsg.notFound)
}

sealed class ArticleCommentError(override val msg: String) : DomainError.Single() {
  object NotFound : ArticleCommentError(ErrorMsg.notFound)
}

sealed class ArticleCommentDeleteError(override val msg: String) : DomainError.Single() {
  object ArticleNotFound : ArticleCommentDeleteError("Article not found")
  object CommentNotFound : ArticleCommentDeleteError("Comment not found")
  object NotAuthor : ArticleCommentDeleteError("Only author can delete")
}
