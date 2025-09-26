package de.solidblocks.cli

import de.solidblocks.cli.hetzner.asg.*
import de.solidblocks.hetzner.cloud.HetznerApi
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class HetznerAsgTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)

    @OptIn(ExperimentalTime::class)
    fun createUserData() = """
        #!/usr/bin/env bash
        # ${Clock.System.now()}
        apt-get update
        apt-get -y install nginx
    """.trimIndent()

    @Test
    fun testInvalidLoadBalancerName() {
        HetznerAsg(hcloudToken).rotate(
            LoadBalancerReference("invalid"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        ) shouldBe ASG_ROTATE_STATUS.LOADBALANCER_NOT_FOUND
    }

    @Test
    fun testInvalidLoadBalancerId() {
        HetznerAsg(hcloudToken).rotate(
            LoadBalancerReference("0"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        ) shouldBe ASG_ROTATE_STATUS.LOADBALANCER_NOT_FOUND
    }

    @Test
    fun testAsg1() {
        runBlocking {

            val unmanagedServer = api.servers.get("hcloud-server1")!!
            var targets = api.loadBalancers.get("application1")!!.targets
            targets.map { it.server!!.id } shouldContain unmanagedServer.id
            val initialServerCount = targets.count()

            HetznerAsg(hcloudToken).rotate(
                LoadBalancerReference("application1"),
                LocationReference("nbg1"),
                ServerTypeReference("cx22"),
                ImageReference("debian-12"),
                userData = createUserData(),
            ) shouldBe ASG_ROTATE_STATUS.OK

            targets = api.loadBalancers.get("application1")!!.targets
            targets.map { it.server!!.id } shouldContain unmanagedServer.id
            targets shouldHaveSize initialServerCount

            HetznerAsg(hcloudToken).rotate(
                LoadBalancerReference("application1"),
                LocationReference("nbg1"),
                ServerTypeReference("cx22"),
                ImageReference("debian-12"),
                userData = createUserData(),
                replicas = 2
            ) shouldBe ASG_ROTATE_STATUS.OK

            targets = api.loadBalancers.get("application1")!!.targets
            targets.map { it.server!!.id } shouldContain unmanagedServer.id
            targets shouldHaveSize initialServerCount + 1

            val userData = createUserData()

            HetznerAsg(hcloudToken).rotate(
                LoadBalancerReference("application1"),
                LocationReference("nbg1"),
                ServerTypeReference("cx22"),
                ImageReference("debian-12"),
                userData = userData,
                replicas = 1
            ) shouldBe ASG_ROTATE_STATUS.OK

            targets = api.loadBalancers.get("application1")!!.targets
            targets.map { it.server!!.id } shouldContain unmanagedServer.id
            targets shouldHaveSize initialServerCount

            HetznerAsg(hcloudToken).rotate(
                LoadBalancerReference("application1"),
                LocationReference("nbg1"),
                ServerTypeReference("cx22"),
                ImageReference("debian-12"),
                userData = userData,
                replicas = 1
            ) shouldBe ASG_ROTATE_STATUS.OK

            targets = api.loadBalancers.get("application1")!!.targets
            targets.map { it.server!!.id } shouldContain unmanagedServer.id
            targets shouldHaveSize initialServerCount
        }
    }

    @Test
    fun testAsg1Timeout() {
        HetznerAsg(hcloudToken).rotate(
            LoadBalancerReference("application1"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = "invalid userdata ${UUID.randomUUID()}",
            healthTimeout = 60.seconds,
            totalTimeout = 75.seconds
        ) shouldBe ASG_ROTATE_STATUS.TIMEOUT
    }

    @Test
    fun testAsg2() {
        HetznerAsg(hcloudToken).rotate(
            LoadBalancerReference("application2"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        ) shouldBe ASG_ROTATE_STATUS.OK
    }

    @Test
    fun testAsg3() {
        HetznerAsg(hcloudToken).rotate(
            LoadBalancerReference("application3"),
            LocationReference("nbg1"),
            ServerTypeReference("cx22"),
            ImageReference("debian-12"),
            userData = createUserData(),
        )
    }

}
