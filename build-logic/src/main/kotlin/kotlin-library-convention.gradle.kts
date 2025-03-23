import org.gradle.accessors.dm.LibrariesForLibs

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

plugins {
  kotlin("jvm") apply false
  `java-library`
  `maven-publish`
  signing
  id("org.jetbrains.dokka")
  id("io.github.gradle-nexus.publish-plugin")
}

val libs = the<LibrariesForLibs>()

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      pom {
        name.set(project.name)
        description.set(project.description)
        url.set("http://nthalk.github.io/SeleniumJQuery/")
        licenses {
          license {
            name.set("MIT License")
            url.set("http://www.opensource.org/licenses/mit-license.php")
          }
        }
        developers {
          developer {
            id.set("nthalk")
            name.set("Carl Taylor")
            email.set("carl@etaylor.me")
            roles.add("owner")
            roles.add("developer")
            timezone.set("-8")
          }
        }
        scm {
          connection.set("scm:git:git@github.com:Nthalk/SeleniumJQuery.git")
          developerConnection.set("scm:git:git@github.com:Nthalk/SeleniumJQuery.git")
          url.set("http://nthalk.github.io/SeleniumJQuery/")
          tag.set("selenium-jquery-2.0.0")
        }
      }
    }
  }
}

signing {
  sign(publishing.publications["mavenJava"])
}

dokka {
  moduleName.set(rootProject.name)
  dokkaSourceSets.main {
    includes.from("README.md")
    sourceLink {
      localDirectory.set(file("src/main/kotlin"))
      remoteUrl("https://github.com/IodeSystems/SeleniumJQuery/blob/main/src/main/kotlin")
      remoteLineSuffix.set("#L")
    }
  }
}
