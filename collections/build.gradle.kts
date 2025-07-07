plugins {
  id("sync.common-conventions")
  alias(libs.plugins.jmh)
}

dependencies {
  compileOnlyApi(libs.jetbrainsAnnotations)
}
