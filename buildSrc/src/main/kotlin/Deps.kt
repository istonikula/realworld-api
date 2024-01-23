object Version {
  const val arrow = "1.2.1"
  const val flyway = "8.0.2"
  const val groovy = "3.0.8"
  const val jacksonKotlin =  "2.13.0"
  const val jasypt = "1.9.3"
  const val java = "17"
  const val jaxb = "2.3.1"
  const val jjwt = "0.9.1"
  const val kotlin = "1.9.22"
  const val kotlinCoroutines = "1.7.3"
  const val restAssured = "4.4.0"
  const val slugify = "2.5"
  const val springBoot = "2.5.6"
  const val versionsPlugin = "0.39.0"
}

object Libs {
  const val arrowCore = "io.arrow-kt:arrow-core:${Version.arrow}"

  const val jacksonKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jacksonKotlin}"
  const val jasypt = "org.jasypt:jasypt:${Version.jasypt}"
  const val jaxb = "javax.xml.bind:jaxb-api:${Version.jaxb}"
  const val jjwt = "io.jsonwebtoken:jjwt:${Version.jjwt}"
  const val jsonSchemaValidator = "io.rest-assured:json-schema-validator:${Version.restAssured}"
  const val junitJupiter = "org.junit.jupiter:junit-jupiter"
  const val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinCoroutines}"
  const val postgresql = "org.postgresql:postgresql"
  const val restassured = "io.rest-assured:rest-assured:${Version.restAssured}"
  const val slugify = "com.github.slugify:slugify:${Version.slugify}"
}

object Starters {
  const val actuator = "org.springframework.boot:spring-boot-starter-actuator"
  const val jdbc = "org.springframework.boot:spring-boot-starter-jdbc"
  const val test = "org.springframework.boot:spring-boot-starter-test"
  const val undertow = "org.springframework.boot:spring-boot-starter-undertow"
  const val validation = "org.springframework.boot:spring-boot-starter-validation"
  const val web = "org.springframework.boot:spring-boot-starter-web"
}

const val implementation = "implementation"
const val testImplementation = "testImplementation"
const val runtimeOnly = "runtimeOnly"
