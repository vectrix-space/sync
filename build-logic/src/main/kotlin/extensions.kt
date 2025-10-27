import com.diffplug.gradle.spotless.FormatExtension
import net.kyori.indra.git.IndraGitExtension
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named

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
