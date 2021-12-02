import Constants.SPRING_BOOT_VERSION

plugins {
    id("nu.studer.jooq") version "5.2.1"
    id("solidblocks.kotlin-library-conventions")
}

dependencies {

    implementation(project(":solidblocks-base"))

    api("org.springframework.boot:spring-boot-starter-jdbc:$SPRING_BOOT_VERSION")
    api("org.liquibase:liquibase-core:4.3.5")
    api("org.jooq:jooq:3.14.11")

    jooqGenerator("org.jooq:jooq-meta-extensions-liquibase")
    jooqGenerator("org.liquibase:liquibase-core")
    jooqGenerator("org.yaml:snakeyaml:1.28")
    jooqGenerator("org.slf4j:slf4j-jdk14:1.7.30")

    testImplementation("org.springframework.boot:spring-boot-test:$SPRING_BOOT_VERSION")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure:$SPRING_BOOT_VERSION")

    testImplementation("org.apache.derby:derbytools:10.15.2.0")
    testImplementation("org.apache.derby:derby:10.15.2.0")

    testImplementation("org.springframework:spring-test:5.3.6")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.assertj:assertj-core:3.21.0")
}

jooq {
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)

            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    target = org.jooq.meta.jaxb.Target().withPackageName("de.solidblocks.config.db")

                    database.apply {
                        name = "org.jooq.meta.extensions.liquibase.LiquibaseDatabase"
                        properties.add(
                            org.jooq.meta.jaxb.Property().withKey("scripts")
                                .withValue("src/main/resources/db/changelog/db.changelog-master.yaml")
                        )
                        properties.add(org.jooq.meta.jaxb.Property().withKey("includeLiquibaseTables").withValue("false"))
                    }
                }
            }
        }
    }
}
