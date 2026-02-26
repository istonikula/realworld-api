object Version {
  const val arrow = "2.2.1.1"
  const val flyway = "12.0.2"
  const val jasypt = "1.9.3"
  const val java = "21"
  const val jaxb = "4.0.5"
  const val jjwt = "0.13.0"
  const val kotlin = "2.3.10"
  const val kotlinCoroutines = "1.10.2"
  const val restAssured = "6.0.0"
  const val slugify = "3.0.7"
  const val springBoot = "4.0.3"
  const val versionsPlugin = "0.53.0"
}

object Libs {
  const val arrowCore = "io.arrow-kt:arrow-core:${Version.arrow}"

  const val flywayPostgresql = "org.flywaydb:flyway-database-postgresql:${Version.flyway}"
  const val jacksonKotlin = "tools.jackson.module:jackson-module-kotlin"
  const val jasypt = "org.jasypt:jasypt:${Version.jasypt}"
  const val jaxb = "jakarta.xml.bind:jakarta.xml.bind-api:${Version.jaxb}"
  const val jjwt = "io.jsonwebtoken:jjwt:${Version.jjwt}"
  const val jsonSchemaValidator = "io.rest-assured:json-schema-validator:${Version.restAssured}"
  const val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinCoroutines}"
  const val postgresql = "org.postgresql:postgresql"
  const val restassured = "io.rest-assured:rest-assured:${Version.restAssured}"
  const val slugify = "com.github.slugify:slugify:${Version.slugify}"
}

object Starters {
  const val actuator = "org.springframework.boot:spring-boot-starter-actuator"
  const val jdbc = "org.springframework.boot:spring-boot-starter-jdbc"
  const val test = "org.springframework.boot:spring-boot-starter-test"
  const val validation = "org.springframework.boot:spring-boot-starter-validation"
  const val webmvc = "org.springframework.boot:spring-boot-starter-webmvc"
}

const val implementation = "implementation"
const val testImplementation = "testImplementation"
const val runtimeOnly = "runtimeOnly"
const val testRuntimeOnly = "testRuntimeOnly"
