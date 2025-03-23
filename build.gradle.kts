import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.iodesystems.selenium-jquery"
version = "3.2.1-SNAPSHOT"
description =
  "SeleniumJQuery is a tool for writing more effective Selenium tests with the power of jQuery selectors and Kotlin's expressiveness"

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

plugins {
  kotlin("jvm")
  `java-library`
  `maven-publish`
  signing
  id("org.jetbrains.dokka")
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(21)
}

tasks.withType<KotlinCompile>().configureEach {
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



nexusPublishing {
  repositories {
    sonatype()
  }
}


dependencies {
  implementation(Kotlin.stdlib.jdk8)
  implementation(libs.selenium.remote.driver)
  implementation(libs.selenium.support)

  testImplementation(Kotlin.test.junit)
  testImplementation(libs.junit)
  testImplementation(libs.selenium.chrome.driver)
  testImplementation(libs.webdrivermanager) {
    exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    exclude(group = "commons-io", module = "commons-io")
  }
  testImplementation(libs.commons.io)
  testImplementation(libs.bcprov.jdk18on)
  testImplementation(libs.jackson.databind)
}


tasks.register("release") {
  group = "release"
  description = "Releases the current version."
  dependsOn("releaseEnsureNoChanges")
  doLast {
    val version = properties["version"] as String
    if (version.contains("SNAPSHOT")) {
      writeVersion(version.removeSuffix("-SNAPSHOT"))
      "git commit -am 'Release $version'".bash()
      "git tag -a v$version -m 'Release $version'".bash()
    }
    "gradle publish".bash()
    writeVersion(generateVersion("patch") + "-SNAPSHOT", version.removeSuffix("-SNAPSHOT"))
    "git commit -am 'Prepare for next development iteration'".bash()
    "git push origin main".bash()
  }
}

tasks.register("releaseEnsureNoChanges") {
  group = "release"
  description = "Ensures that there are no changes in the working directory."
  doLast {
    val status = "git status --porcelain".bash().trim()
    if (status.isNotEmpty()) {
      throw GradleException("There are changes in the working directory:\n$status")
    }
  }
}

tasks.register("releaseTagAndPushRelease") {
  group = "release"
  description = "Tags the current commit with the current version."
  dependsOn("releaseEnsureNoChanges")
  doLast {
    val version = properties["version"] as String
    "git tag -a v$version -m 'Release $version'".bash()
    "git push origin v$version".bash()
  }
}


fun generateVersion(updateMode: String): String {
  properties["overrideVersion"].let {
    if (it != null) {
      return it as String
    }
  }

  val version = properties["version"] as String
  val nonSnapshotVersion = version.removeSuffix("-SNAPSHOT")

  val (oldMajor, oldMinor, oldPatch) = nonSnapshotVersion.split(".").map(String::toInt)
  var (newMajor, newMinor, newPatch) = arrayOf(oldMajor, oldMinor, 0)

  when (updateMode) {
    "major" -> newMajor = (oldMajor + 1).also { newMinor = 0 }
    "minor" -> newMinor = oldMinor + 1
    "dev" -> newPatch = oldPatch + 1
    else -> newPatch = oldPatch + 1
  }
  if (updateMode == "dev" || nonSnapshotVersion != version) {
    return "$newMajor.$newMinor.$newPatch-SNAPSHOT"
  }
  return "$newMajor.$newMinor.$newPatch"
}

fun writeVersion(newVersion: String, oldVersion: String = version.toString()) {
  val oldContent = buildFile.readText()
  val newContent = oldContent.replace("""= "$oldVersion"""", """= "$newVersion"""")
  buildFile.writeText(newContent)
}

tasks.register("releaseIncrementVersion") {
  group = "release"
  description = "Increments the version in this build file everywhere it is used."
  doLast {
    val version = generateVersion(properties["mode"]?.toString() ?: "patch")
    println("New version: $version")
    writeVersion(version)
  }
}

private fun String.bash(): String {
  val process = ProcessBuilder(
    "bash", "-c", this
  ).start()
  var content = ""
  val er = Thread {
    process.errorStream.reader().useLines { lines ->
      lines.forEach {
        println(it)
      }
    }
  }
  val out = Thread {
    content = process.inputStream.reader().useLines { lines ->
      lines.map {
        println(it)
        it
      }.joinToString("\n")
    }
  }
  er.start()
  out.start()
  process.waitFor().also { code ->
    er.join()
    out.join()
    if (code != 0) {
      throw GradleException("Failed ($code) to execute command: $this")
    }
  }
  return content
}
