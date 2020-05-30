import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.flywaydb.gradle.FlywayExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
  id("com.github.ben-manes.versions") version Version.versionsPlugin
  id("org.flywaydb.flyway") version Version.flyway apply false
  id("org.jetbrains.kotlin.jvm") version Version.kotlin apply false
  id("org.jetbrains.kotlin.plugin.spring") version Version.kotlin apply false
  id("org.jlleitschuh.gradle.ktlint") version Version.ktlintPlugin apply false
  id("org.springframework.boot") version Version.springBoot apply false
}

configure(subprojects.apply {
  remove(project(":realworld-app"))
  remove(project(":realworld-infra"))
}) {
  apply(plugin = "io.spring.dependency-management")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.kotlin.plugin.spring")
  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  version = "0.0.1-SNAPSHOT"

  repositories {
    jcenter()
    mavenCentral()
  }

  configurations {
    all {
      resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
          useVersion(Version.kotlin)
          because("use single kotlin version")
        }
      }
    }
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = Version.java
      freeCompilerArgs = listOf(
        "-Xjsr305=strict",
        "-XXLanguage:+InlineClasses"
      )
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    outputs.upToDateWhen { false }

    testLogging {
      events("passed", "failed", "skipped")
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  configure<KtlintExtension> {
    version.set(Version.ktlint)
  }

  dependencies {
    implementation.let {
      // override spring-boot platform versions
      project.extra.set("rest-assured.version", Version.restAssured)
      it(platform("org.springframework.boot:spring-boot-dependencies:${Version.springBoot}"))

      it(Libs.arrowFx)
      it(Libs.arrowMtl)
      it(Libs.arrowSyntax)

      it(Libs.kotlinStd)
      it(Libs.kotlinReflect)
    }

    runtime(Libs.jaxb)

    testImplementation.let {
      it(Starters.test) {
        exclude(group = "junit", module = "junit")
      }

      it(Libs.junitJupiterApi)
      it(Libs.junitJupiterEngine)
    }
  }
}

project("realworld-app:web") {
  apply(plugin = "org.springframework.boot")
  apply(plugin = "org.flywaydb.flyway")

  configure<FlywayExtension> {
    url = "jdbc:postgresql://localhost:5432/realworld"
    user = "postgres"
    password = "secret"
    placeholders = mapOf(
      "application_user" to "realworld"
    )
  }

  dependencies {
    implementation.let {
      it(project(":realworld-domain"))
      it(project(":realworld-infra:persistence"))

      it(Starters.actuator)
      it(Starters.jdbc)
      it(Starters.web) {
        exclude(
          group = "org.springframework.boot",
          module = "spring-boot-starter-tomcat"
        )
      }
      it(Starters.undertow)

      it(Libs.jacksonKotlin)
    }

    runtime(Libs.postgresql)

    testImplementation.let {
      it(Libs.jsonSchemaValidator)
      it(Libs.restassured)
    }
  }
}

project("realworld-domain") {
  dependencies {
    implementation.let {
      it(Libs.jasypt)
      it(Libs.jjwt)
      it(Libs.slugify)
    }
  }
}

project("realworld-infra:persistence") {
  dependencies {
    implementation.let {
      it(project(":realworld-domain"))

      it(Starters.jdbc)

      it(Libs.postgresql)
    }
  }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
  resolutionStrategy {
    componentSelection {
      all {
        val rejected = listOf("alpha", "b", "beta", "build-snapshot", "rc", "cr", "m", "preview")
          .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
          .any { it.matches(candidate.version) }
        if (rejected) {
          reject("Release candidate")
        }
      }
    }
  }
  checkForGradleUpdate = true
}
