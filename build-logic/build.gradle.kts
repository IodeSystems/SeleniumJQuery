repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

plugins {
  `kotlin-dsl`
  alias(libs.plugins.gradle.nexus.publish)
}

dependencies {
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.dokka.gradle.plugin)
  compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
