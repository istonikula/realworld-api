package io.realworld

import io.realworld.domain.api.RegisterUser
import io.realworld.domain.api.UserService
import io.realworld.domain.api.dto.UserDto
import io.realworld.domain.api.event.AuthenticateEvent
import io.realworld.domain.core.Auth
import io.realworld.domain.core.CoreUserService
import io.realworld.domain.core.RegisterUserWorkflow
import io.realworld.domain.core.ValidateUserRegistrationBean
import io.realworld.domain.spi.Settings
import io.realworld.domain.spi.ValidateUserRegistration
import io.realworld.persistence.InMemoryUserRepository
import ma.glasnost.orika.Converter
import ma.glasnost.orika.Mapper
import ma.glasnost.orika.MapperFactory
import ma.glasnost.orika.converter.builtin.PassThroughConverter
import ma.glasnost.orika.impl.ConfigurableMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.stream.Stream

@SpringBootApplication
class Spring5Application {
  @Bean
  @ConfigurationProperties(prefix = "realworld")
  fun settings() = Settings()

  @Bean
  fun orikaBeanMapper() = OrikaBeanMapper()

  @Bean
  fun userArgumentResolver() = UserArgumentResolver(userService())

  @Bean
  fun userService() = CoreUserService(auth(), userRepository())

  @Bean
  fun auth() = Auth(settings().security)

  @Bean
  fun userRepository() = InMemoryUserRepository()

  @Bean
  fun validateUserRegistration(): ValidateUserRegistration = ValidateUserRegistrationBean(userRepository())

  @Bean
  fun registerUser(): RegisterUser = RegisterUserWorkflow(auth(), userRepository(), validateUserRegistration())
}

fun main(args: Array<String>) {
  SpringApplication.run(Spring5Application::class.java, *args)
}

class OrikaBeanMapper : ConfigurableMapper(false) {

  private lateinit var appCtx: ApplicationContext

  @Autowired
  fun initWith(appCtx: ApplicationContext) {
    this.appCtx = appCtx
    init()
  }

  override fun configure(factory: MapperFactory) {
    FACTORY = factory

    appCtx.getBeansOfType(Mapper::class.java).values.forEach { x -> factory.registerMapper(x) }
    appCtx.getBeansOfType(Converter::class.java).values.forEach { x -> factory.converterFactory.registerConverter(x) }

    Stream.of(
      ZonedDateTime::class.java
    ).forEach { x -> factory.converterFactory.registerConverter(PassThroughConverter(x)) }
  }

  companion object {
    lateinit var FACTORY: MapperFactory
  }
}

class UserArgumentResolver(val userService: UserService) : HandlerMethodArgumentResolver {
  override fun supportsParameter(parameter: MethodParameter?): Boolean =
    UserDto::class.java.isAssignableFrom(parameter?.parameterType)

  override fun resolveArgument(
    parameter: MethodParameter?,
    bindingContext: BindingContext?,
    exchange: ServerWebExchange?
  ): Mono<Any> {
    val authorization: String? = exchange?.request?.headers?.getFirst(HttpHeaders.AUTHORIZATION)
    authorization?.apply {
      if (startsWith(TOKEN_PREFIX)) {
        return try {
          val user: UserDto = userService.authenticate(AuthenticateEvent(substring(TOKEN_PREFIX.length))).user
          Mono.just(user)
        } catch (t: Throwable) {
          throw UnauthrorizedException()
        }
      }
    }
    throw UnauthrorizedException()
  }

  companion object {
    private val TOKEN_PREFIX = "Token "
  }
}

class UnauthrorizedException : Throwable()
