object Version {
  const val arrow = "1.0.0"
  const val flyway = "6.5.5"
  const val groovy = "3.0.4"
  const val jacksonKotlin =  "2.11.2"
  const val jasypt = "1.9.3"
  const val java = "11"
  const val jaxb = "2.3.1"
  const val jjwt = "0.9.1"
  const val kotlin = "1.5.31"
  const val kotlinCoroutines = "1.5.2"
  const val ktlint = "0.38.1"
  const val ktlintPlugin = "9.3.0"
  const val restAssured = "4.3.1"
  const val slugify = "2.4"
  const val springBoot = "2.3.3.RELEASE"
  const val versionsPlugin = "0.30.0"
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
const val runtime = "runtime"
