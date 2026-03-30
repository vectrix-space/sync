@file:Suppress("UnstableApiUsage", "unused")

import com.diffplug.gradle.spotless.FormatExtension
import net.kyori.indra.git.IndraGitExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.jvm.JvmComponentDependencies
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.testing.base.TestingExtension

val Project.libs: LibrariesForLibs
  get() = project.extensions.getByType(LibrariesForLibs::class)

fun Project.addTestSuite(suiteName: String, dependencyAction: Action<JvmComponentDependencies>) {
  val tests = project.extensions.findByType<TestingExtension>()
  tests?.suites?.create(suiteName, JvmTestSuite::class) {
    useJUnitJupiter()

    dependencies {
      dependencyAction(this)
    }
  }

  tasks.named("test") {
    dependsOn(tasks.named(suiteName))
  }
}

fun Project.applyJarMetadata(moduleName: String) {
  if("jar" in tasks.names) {
    tasks.named<Jar>("jar") {
      manifest.attributes(
        "Automatic-Module-Name" to moduleName,
        "Specification-Title" to moduleName,
        "Specification-Version" to project.name,
        "Specification-Vendor" to "vectrix.space"
      )

      val indraGit = project.extensions.findByType<IndraGitExtension>()
      indraGit?.applyVcsInformationToManifest(manifest)
    }
  }
}

fun FormatExtension.applyCommon() {
  trimTrailingWhitespace()
  endWithNewline()
  leadingTabsToSpaces(2)
}
