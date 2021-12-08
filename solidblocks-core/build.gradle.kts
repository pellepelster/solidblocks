plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
    api("joda-time:joda-time:2.10.10")

    testImplementation("org.hamcrest:hamcrest:2.2")
}
