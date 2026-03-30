@file:Suppress("UnstableApiUsage")

plugins {
  id("sync.common-conventions")
  alias(libs.plugins.jmh)
}

dependencies {
  compileOnlyApi(libs.jetbrainsAnnotations)
  compileOnlyApi(libs.jspecify)
}

addTestSuite("googleTest") {
  implementation(project(":sync-collections"))
  implementation(libs.guava.testlib)

  implementation(platform(libs.junit.bom))
  implementation(libs.junit.vintage)

  implementation(libs.junit.legacy)
}

applyJarMetadata("space.vectrix.sync.collections")
