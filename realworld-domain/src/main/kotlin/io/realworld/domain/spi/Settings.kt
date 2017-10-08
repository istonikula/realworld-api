package io.realworld.domain.spi

class Settings {
  val security = Security()

  class Security {
    lateinit var tokenSecret: String
  }
}
