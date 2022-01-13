plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-vault"))
    api(project(":solidblocks-base"))

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    testImplementation(project(":solidblocks-test"))
}
