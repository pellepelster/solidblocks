package de.solidblocks.cli

import de.solidblocks.cli.hetzner.asg.*
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class HetznerAsgTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()

    @OptIn(ExperimentalTime::class)
    fun createUserData() = """
        #!/usr/bin/env bash
        # ${Clock.System.now()}
        apt-get update
        apt-get -y install nginx
    """.trimIndent()

    @Test
    fun testInvalidLoadBalancerName() {
        HetznerAsg(hcloudToken).run(
            LoadBalancerReference("invalid"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        ) shouldBe ASG_ROTATE_STATUS.LOADBALANCER_NOT_FOUND
    }

    @Test
    fun testInvalidLoadBalancerId() {
        HetznerAsg(hcloudToken).run(
            LoadBalancerReference("0"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        ) shouldBe ASG_ROTATE_STATUS.LOADBALANCER_NOT_FOUND
    }

    @Test
    fun testAsg1() {
        HetznerAsg(hcloudToken).run(
            LoadBalancerReference("application1"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        ) shouldBe ASG_ROTATE_STATUS.OK

        HetznerAsg(hcloudToken).run(
            LoadBalancerReference("application1"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        ) shouldBe ASG_ROTATE_STATUS.OK
    }

    @Test
    fun testAsg1Timeout() {
        HetznerAsg(hcloudToken).run(
            LoadBalancerReference("application1"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = "invalid userdata ${UUID.randomUUID()}",
            healthTimeout = 45.seconds,
            totalTimeout = 60.seconds
        ) shouldBe ASG_ROTATE_STATUS.TIMEOUT
    }

    @Test
    fun testAsg2() {
        HetznerAsg(hcloudToken).run(
            LoadBalancerReference("application2"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = "invalid userdata ${UUID.randomUUID()}",
        ) shouldBe ASG_ROTATE_STATUS.TIMEOUT
    }

    @Test
    fun testAsg3() {
        HetznerAsg(hcloudToken).run(
            LoadBalancerReference("application3"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        )
    }

}
