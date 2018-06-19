package io.realworld

import arrow.core.getOrElse
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Settings
import io.realworld.domain.users.UserRepository
import io.realworld.persistence.JdbcUserRepository
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@SpringBootApplication
class Spring5Application {
  @Bean
  @ConfigurationProperties(prefix = "realworld")
  fun settings() = Settings()

  @Bean
  fun userArgumentResolverBean(repo: UserRepository) = userArgumentResolver(
    JwtTokenResolver(auth()::parse),
    { token ->
      repo.findById(token.id).unsafeRunSync().map { it.user }.getOrElse { throw UnauthorizedException() }
    }
  )

  @Bean
  fun auth() = Auth(settings().security)

  @Bean
  fun userRepository(jdbcTemplate: NamedParameterJdbcTemplate) = JdbcUserRepository(jdbcTemplate)
}

fun main(args: Array<String>) {
  SpringApplication.run(Spring5Application::class.java, *args)
}


inline fun <reified User, reified Token> userArgumentResolver(
  crossinline resolveToken: ResolveToken<Token>,
  crossinline createUser: (token: Token) -> User
) = object : HandlerMethodArgumentResolver {
  override fun supportsParameter(parameter: MethodParameter) =
    User::class.java.isAssignableFrom(parameter.parameterType)

  override fun resolveArgument(
    parameter: MethodParameter,
    bindingContext: BindingContext,
    exchange: ServerWebExchange
  ) = with(exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)) {
    resolveToken(this).fold(
      { throw UnauthorizedException() },
      { Mono.just(createUser(it) as Any) }
    )
  }
}

class UnauthorizedException : Throwable()
