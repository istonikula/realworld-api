package io.realworld

import arrow.core.getOrElse
import io.realworld.domain.common.Auth
import io.realworld.domain.common.DomainError
import io.realworld.domain.common.Settings
import io.realworld.domain.common.Token
import io.realworld.domain.users.User
import io.realworld.errors.RestException
import io.realworld.persistence.ArticleRepository
import io.realworld.persistence.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.MethodParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
class Application : WebMvcConfigurer {
  @Bean
  @ConfigurationProperties(prefix = "realworld")
  fun settings() = Settings()

  @Bean
  fun userArgumentResolverBean() = userArgumentResolver(
    JwtTokenResolver(auth()::parse),
    userCreator()
  )

  @Bean
  fun userCreator() = object : (Token) -> User {
    @Autowired
    lateinit var repo: UserRepository

    // TODO check token match
    override fun invoke(token: Token): User {
      return repo.findById(token.id).unsafeRunSync().map { it.user }.getOrElse {
        throw RestException.Unauthorized(AuthError.BadCredentials)
      }
    }
  }

  @Bean
  fun auth() = Auth(settings().security)

  @Bean
  fun userRepository(jdbcTemplate: NamedParameterJdbcTemplate) = UserRepository(jdbcTemplate)

  @Bean
  fun articleRepository(jdbcTemplate: NamedParameterJdbcTemplate, userRepository: UserRepository) = ArticleRepository(
    jdbcTemplate, userRepository
  )

  override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
    resolvers.add(userArgumentResolverBean())
  }
}

fun main(args: Array<String>) {
  SpringApplication.run(Application::class.java, *args)
}

inline fun <reified User, reified Token> userArgumentResolver(
  crossinline resolveToken: ResolveToken<Token>,
  crossinline createUser: (token: Token) -> User
) = object : HandlerMethodArgumentResolver {
  override fun supportsParameter(parameter: MethodParameter) =
    User::class.java.isAssignableFrom(parameter.parameterType)

  override fun resolveArgument(
    parameter: MethodParameter,
    mavContainer: ModelAndViewContainer?,
    webRequest: NativeWebRequest,
    binderFactory: WebDataBinderFactory?
  ) = resolveToken(webRequest.authHeader()).fold(
    { throw RestException.Unauthorized(it) },
    { createUser(it) }
  )
}

sealed class AuthError(override val msg: String) : DomainError.Single() {
  object InvalidToken : AuthError("Invalid token")
  object InvalidAuthorizationHeader : AuthError("Invalid authorization header")
  object BadCredentials : AuthError("Bad credentials")
}
