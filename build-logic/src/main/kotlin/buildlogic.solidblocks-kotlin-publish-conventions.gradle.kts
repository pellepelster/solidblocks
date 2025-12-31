plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "test"
            url = uri("s3://test-blcks-bootstrap.s3.eu-central-1.amazonaws.com")

            credentials(AwsCredentials::class) {
                accessKey = providers.of(PassSecretValueSource::class) {
                    this.parameters.path = "solidblocks/aws/test/access_key_id"
                }.get()

                secretKey = providers.of(PassSecretValueSource::class) {
                    this.parameters.path = "solidblocks/aws/test/secret_access_key"
                }.get()
            }
        }
    }
}

