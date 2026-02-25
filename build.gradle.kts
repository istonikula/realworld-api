import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.flywaydb.gradle.FlywayExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
        // Force coroutines version to be compatible with Kotlin
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines")) {
          useVersion(Version.kotlinCoroutines)
          because("use compatible coroutines version")
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
    // override spring-boot platform versions
    project.extra.set("groovy.version", Version.groovy) // keep in sync with rest-assured https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt
    project.extra.set("rest-assured.version", Version.restAssured)

    implementation.let {
      it(platform("org.springframework.boot:spring-boot-dependencies:${Version.springBoot}"))

      it(Libs.kotlinCoroutines)
      it(Libs.arrowCore)
    }

    runtimeOnly(Libs.jaxb)

    testImplementation.let {
      it(Starters.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
      }
      // Add junit-platform-launcher to ensure Gradle can run tests
      it("org.junit.platform:junit-platform-launcher")
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
      it(Starters.validation)
      it(Starters.web)
      // Removed undertow exclusion and starter to fallback to default (Tomcat)
      // it(Starters.undertow)

      it(Libs.jacksonKotlin)
      it("org.jetbrains.kotlin:kotlin-reflect")
    }

    runtimeOnly(Libs.postgresql)

    testImplementation.let {
      it(Libs.jsonSchemaValidator)
      it(Libs.restassured)
      // Enable Jackson 2 modules for Test deserialization (RestAssured)
      it(Libs.jackson2Kotlin)
      it(Libs.jackson2Jsr310)
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
