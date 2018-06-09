package io.realworld.domain.common

class Settings {
  val security = Security()

  class Security {
    lateinit var tokenSecret: String
  }
}
