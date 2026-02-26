import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent
import org.flywaydb.gradle.FlywayExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(Libs.flywayPostgresql)
  }
}

plugins {
  id("com.github.ben-manes.versions") version Version.versionsPlugin
  id("org.flywaydb.flyway") version Version.flyway apply false
  id("org.jetbrains.kotlin.jvm") version Version.kotlin apply false
  id("org.jetbrains.kotlin.plugin.spring") version Version.kotlin apply false
  id("org.springframework.boot") version Version.springBoot apply false
}

configure(subprojects.apply {
  remove(project(":realworld-app"))
  remove(project(":realworld-infra"))
}) {
  apply(plugin = "io.spring.dependency-management")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.kotlin.plugin.spring")

  version = "0.0.1-SNAPSHOT"

  repositories {
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

  val javaLauncher = project.extensions.getByType<JavaToolchainService>().launcherFor {
    languageVersion.set(JavaLanguageVersion.of(Version.java))
  }
  tasks.withType<KotlinCompile> {
    kotlinJavaToolchain.toolchain.use(javaLauncher)
    compilerOptions {
      freeCompilerArgs.add("-Xjsr305=strict")
      freeCompilerArgs.add("-Xinline-classes")
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

  dependencies {
    implementation.let {
      it(platform("org.springframework.boot:spring-boot-dependencies:${Version.springBoot}"))

      it(Libs.kotlinCoroutines)
      it(Libs.arrowCore)
    }

    runtimeOnly(Libs.jaxb)

    testImplementation(Starters.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
      it(Starters.validation)
      it(Starters.webmvc)

      it(Libs.jacksonKotlin)
    }

    runtimeOnly(Libs.postgresql)

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
      all(Action<ComponentSelectionWithCurrent> {
        val rejected = listOf("alpha", "b", "beta", "build-snapshot", "rc", "cr", "m", "preview")
          .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
          .any { it.matches(candidate.version) }
        if (rejected) {
          reject("Release candidate")
        }
      })
    }
  }
  checkForGradleUpdate = true
}
