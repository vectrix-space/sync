import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(libs.build.indra)
  implementation(libs.build.indra.sonatype)
  implementation(libs.build.indra.spotless)
  compileOnly(libs.build.jmh)
}

dependencies {
  compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
  target {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_11
    }
  }
}
