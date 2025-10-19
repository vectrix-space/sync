plugins {
  id("sync.common-conventions")
  alias(libs.plugins.jmh)
}

dependencies {
  compileOnlyApi(libs.jetbrainsAnnotations)
}

applyJarMetadata("space.vectrix.sync.collections")
