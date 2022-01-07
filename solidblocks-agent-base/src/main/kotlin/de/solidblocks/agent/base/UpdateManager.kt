package de.solidblocks.agent.base

import okhttp3.OkHttpClient

//        "https://maven.pkg.github.com/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.jar" > "${temp_file}"

class UpdateManager(val urlTemplate: String, val currentVersion: String) {
    val httpClient = OkHttpClient()
}
