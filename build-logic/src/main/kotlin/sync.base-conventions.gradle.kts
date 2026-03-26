plugins {
  id("net.kyori.indra")
  id("net.kyori.indra.publishing")
}

var libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

indra {
  javaVersions {
    minimumToolchain(17)
    target(17)

    val testVersions = (project.property("testJdks") as String)
      .split(",")
      .map { it.trim().toInt() }

    testWith().addAll(testVersions)
  }

  checkstyle(libs.versions.checkstyle.get())

  github("vectrix-space", "sync") {
    ci(true)
  }

  mitLicense()

  signWithKeyFromPrefixedProperties("vectrix")
  configurePublications {
    pom {
      developers {
        developer {
          id = "VectrixDevelops"
          name = "Vectrix"
        }
      }
    }
  }
}
