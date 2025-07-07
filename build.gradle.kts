plugins {
  alias(libs.plugins.indra.sonatype)
  alias(libs.plugins.nexusPublish)
}

// Project metadata is configured in gradle.properties

nexusPublishing {
  val sonatypeUsername: String = (findProperty("sonatypeUsername") as? String) ?: ""
  val sonatypePassword: String = (findProperty("sonatypePassword") as? String) ?: ""

  repositories {
    named("sonatype") {
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
      username.set(sonatypeUsername)
      password.set(sonatypePassword)
    }
  }
}
