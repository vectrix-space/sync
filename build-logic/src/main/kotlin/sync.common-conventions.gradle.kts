import me.champeau.jmh.JMHPlugin
import me.champeau.jmh.JmhParameters

plugins {
  id("sync.base-conventions")
  id("net.kyori.indra.checkstyle")
  id("net.kyori.indra.licenser.spotless")
}

val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

plugins.withId("me.champeau.jmh") {
  extensions.configure(JmhParameters::class) {
    jmhVersion = libs.versions.jmh.get()

    jvm.set(
      javaToolchains
        .launcherFor {
          languageVersion.set(JavaLanguageVersion.of(21))
        }
        .map { launcher ->
          launcher.executablePath
            .asFile
            .absolutePath
        }
    )
  }

  tasks.named("compileJmhJava") {
    dependsOn(tasks.compileTestJava, tasks.processTestResources)
  }

  tasks.named(JMHPlugin.getJMH_TASK_COMPILE_GENERATED_CLASSES_NAME(), JavaCompile::class) {
    classpath += configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).incoming.files
  }
}

dependencies {
  checkstyle(libs.stylecheck)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.engine)
  testImplementation(libs.junit.params)
  testRuntimeOnly(libs.junit.launcher)
}

spotless {
  java {
    importOrderFile(rootProject.file(".spotless/vectrix.importorder"))
    targetExclude("build/generated/**")
    applyCommon()
  }

  kotlin {
    applyCommon()
  }
}


