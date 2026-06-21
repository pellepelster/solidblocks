plugins {
    `maven-publish`
}

// Publishes the module's jar to the local maven repository (~/.m2) via `publishToMavenLocal`.
// Coordinates default to group `de.solidblocks` (set in the kotlin conventions), the project
// name as artifactId and the `VERSION` env var (or `0.0.0`) as version.
publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
