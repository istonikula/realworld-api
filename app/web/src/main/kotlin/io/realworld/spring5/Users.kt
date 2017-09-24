package io.realworld.spring5

import com.fasterxml.jackson.annotation.JsonRootName
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@JsonRootName("user")
data class Login (
  val email: String,
  val password: String
)

@JsonRootName("user")
data class User(
    val email: String,
    val token: String,
    val username: String,
    val bio: String? = null,
    val image: String? = null
)

@RestController
class UserController {

  @PostMapping("/api/users/login")
  fun login(@RequestBody login: Login) : User {
    return User(
        email = login.email,
        token = "token",
        username = login.email
    )
  }
}
