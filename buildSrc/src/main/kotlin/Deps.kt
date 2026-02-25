object Version {
  const val arrow = "1.2.1"
  const val flyway = "9.22.3"
  const val groovy = "4.0.16"
  const val jacksonKotlin =  "3.0.3"
  const val jackson2Kotlin = "2.16.1"
  const val jasypt = "1.9.3"
  const val java = "21"
  const val jaxb = "4.0.0"
  const val jjwt = "0.12.3"
  const val kotlin = "2.3.10"
  const val kotlinCoroutines = "1.10.2"
  const val restAssured = "5.4.0"
  const val slugify = "3.0.6"
  const val springBoot = "4.0.3"
  const val versionsPlugin = "0.51.0"
}

object Libs {
  const val arrowCore = "io.arrow-kt:arrow-core:${Version.arrow}"

  // Jackson 3 modules for App
  const val jacksonKotlin = "tools.jackson.module:jackson-module-kotlin:${Version.jacksonKotlin}"

  // Jackson 2 modules for Tests (RestAssured)
  const val jackson2Kotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson2Kotlin}"
  const val jackson2Jsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Version.jackson2Kotlin}"

  const val jasypt = "org.jasypt:jasypt:${Version.jasypt}"
  const val jaxb = "jakarta.xml.bind:jakarta.xml.bind-api:${Version.jaxb}"
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
