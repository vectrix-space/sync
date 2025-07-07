plugins {
  alias(libs.plugins.indra.sonatype)
  alias(libs.plugins.nexusPublish)
}

// Project metadata is configured in gradle.properties

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
      username.set(findProperty("sonatypeUsername") as String)
      password.set(findProperty("sonatypePassword") as String)
    }
  }
}
