package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.provisioner.mock.*
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDataLookupProviderTest {

  @Test
  fun testLookup() {
    val provisioner1 = Resource1Provisioner()
    val provisioner2 = Resource2Provisioner()

    runBlocking {
      val registry =
          ProvisionersRegistry(
              listOf(
                  provisioner1,
                  provisioner2,
                  UserDataLookupProvider(),
              ),
              listOf(provisioner1, provisioner2),
          )
      val context = TEST_PROVISIONER_CONTEXT.copy(registry = registry)

      val resource1 = Resource1("resource1")
      val resource2 = Resource2("resource2")

      registry.apply<Resource1, Resource1Runtime>(resource1, context, TEST_LOG_CONTEXT)
      registry.apply<Resource2, Resource2Runtime>(resource2, context, TEST_LOG_CONTEXT)

      val userData =
          UserData(
              emptySet(),
              {
                val a = it.ensureLookup(resource1.asLookup())
                val b = it.ensureLookup(resource2.asLookup())

                "${a.name}:${b.name}"
              },
          )

      registry.lookup(userData, context)!!.userData shouldBe "resource1:resource2"
    }
  }
}
