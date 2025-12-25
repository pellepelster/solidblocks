package de.solidblocks.webs3docker.test

import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationPublicDockerTest : BaseIntegrationTest() {

  @BeforeAll
  fun setup(context: SolidblocksTestContext) {
    init(context, true, true)
  }

  @Test
  fun testDockerAnonymousUserCanNotPullFromPrivate() {
    val exception =
        shouldThrow<Exception> {
          docker
              .pullImageCmd("$dockerHostPrivate/alpine")
              .withTag(imageTag)
              .exec(PullImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldContain ("no basic auth credentials")
  }

  @Test
  fun testDockerAnonymousUserCanPullFromPublic() {
    docker
        .pullImageCmd("$dockerHostPublic/alpine")
        .withTag(imageTag)
        .exec(PullImageResultCallback())
        .awaitCompletion()
  }

  @Test
  fun testDockerAnonymousUserCanNotPushToPrivate() {
    val exception =
        shouldThrow<Exception> {
          docker
              .pushImageCmd("$dockerHostPrivate/alpine")
              .withTag(imageTag)
              .exec(PushImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldContain ("Could not push image: no basic auth credentials")
  }

  @Test
  fun testDockerAnonymousUserCanNotPushToPublic() {
    val exception =
        shouldThrow<Exception> {
          docker
              .pushImageCmd("$dockerHostPrivate/alpine")
              .withTag(imageTag)
              .exec(PushImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldContain ("Could not push image: no basic auth credentials")
  }
}
