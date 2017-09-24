package io.realworld.spring5

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Spring5Application

fun main(args: Array<String>) {
  SpringApplication.run(Spring5Application::class.java, *args)
}
