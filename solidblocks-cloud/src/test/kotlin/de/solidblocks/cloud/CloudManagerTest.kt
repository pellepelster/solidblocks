package de.solidblocks.cloud

/*
@ExtendWith(SolidblocksTestDatabaseExtension::class)
class CloudManagerTest {

    @Test
    fun testService(solidblocksDatabase: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)

        val cloud = cloudRepository.createCloud("cloud1", "domain1")
        Assertions.assertThat(environmentRepository.createEnvironment(cloud.name, "env1")).isNotNull

        val serviceRepository = ServiceRepository(solidblocksDatabase.dsl, environmentRepository)

        Assertions.assertThat(serviceRepository.hasService("cloud1", "env1", "service1")).isFalse

        serviceRepository.createService("cloud1", "env1", "service1")


    }
}

*/
