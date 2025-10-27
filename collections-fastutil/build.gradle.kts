import groovy.text.SimpleTemplateEngine
import net.kyori.blossom.SourceTemplateSet

plugins {
  id("sync.common-conventions")
  alias(libs.plugins.jmh)
  alias(libs.plugins.blossom)
}

dependencies {
  compileOnlyApi(libs.jetbrainsAnnotations)
  compileOnlyApi(libs.jspecify)

  compileOnlyApi(libs.fastutil)

  testImplementation(libs.fastutil)
}

val licenseHeader: Provider<String> = providers.provider {
  val props = HashMap(indraSpotlessLicenser.properties().get())
  val text = SimpleTemplateEngine()
    .createTemplate(rootDir.resolve("license_header.txt"))
    .make(props)
    .toString()
    .trim()

  val ln = System.lineSeparator()
  text
    .split(Regex("\\r?\\n"))
    .joinToString(
      separator = ln,
      prefix = "/*$ln",
      postfix = "$ln */"
    ) { if (it.isEmpty()) " *" else " * $it" }
}

val templateVariables = layout.projectDirectory.file("src/templateData/template-variables.yaml")

sourceSets {
  main {
    blossom {
      javaSources {
        propertyFile(templateVariables)
        variants("double", "float", "int", "short", "long")
      }
    }
  }

  test {
    blossom {
      javaSources {
        propertyFile(templateVariables)
        variants("double", "float", "int", "short", "long")
      }
    }
  }

  configureEach {
    blossom.templateSets.withType(SourceTemplateSet::class).configureEach {
      header = licenseHeader
    }
  }
}

spotless {
  format("generatedJava") {
    target("build/generated/sources/blossom/**/*.java")
    applyCommon()
  }
}

tasks.named("spotlessGeneratedJava").configure {
  listOf("generateJavaTemplates", "generateTestJavaTemplates")
    .mapNotNull { tasks.findByName(it) }
    .forEach { dependsOn(it) }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:-cast"))
}

applyJarMetadata("space.vectrix.sync.collections.fastutil")
