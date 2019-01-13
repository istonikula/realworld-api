package io.realworld

import arrow.core.getOrElse
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monadDefer.monadDefer
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Settings
import io.realworld.domain.common.Token
import io.realworld.domain.users.User
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
    lateinit var repo: UserRepository<ForIO>

    override fun invoke(token: Token): User {
      return repo.findById(token.id).fix().unsafeRunSync().map { it.user }.getOrElse { throw UnauthorizedException() }
    }
  }

  @Bean
  fun auth() = Auth(settings().security)

  @Bean
  fun userRepository(jdbcTemplate: NamedParameterJdbcTemplate) = UserRepository(jdbcTemplate, IO.monadDefer())

  @Bean
  fun articleRepository(
    jdbcTemplate: NamedParameterJdbcTemplate,
    userRepository: UserRepository<ForIO>
  ) = ArticleRepository(
    jdbcTemplate, userRepository, IO.monadDefer()
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
    { throw UnauthorizedException() },
    { createUser(it) }
  )
}

class ForbiddenException : Throwable()
class UnauthorizedException : Throwable()
