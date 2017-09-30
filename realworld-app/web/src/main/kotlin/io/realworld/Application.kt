package io.realworld

import ma.glasnost.orika.Converter
import ma.glasnost.orika.Mapper
import ma.glasnost.orika.MapperFactory
import ma.glasnost.orika.converter.builtin.PassThroughConverter
import ma.glasnost.orika.impl.ConfigurableMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.stream.Stream

@SpringBootApplication
class Spring5Application

fun main(args: Array<String>) {
  SpringApplication.run(Spring5Application::class.java, *args)
}

@Component
class OrikaBeanMapper : ConfigurableMapper(false) {

  private lateinit var appCtx: ApplicationContext

  @Autowired
  fun triggerInit(appCtx: ApplicationContext) {
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
