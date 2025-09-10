package de.solidblocks.cli

import de.solidblocks.cli.hetzner.asg.HetznerAsg
import de.solidblocks.cli.hetzner.asg.LoadBalancerReference
import org.junit.jupiter.api.Test

class HetznerAsgTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()

    val userData = """
        #!/usr/bin/env bash

        apt-get update
        apt-get -y install nginx
    """.trimIndent()

    @Test
    fun testAsg1() {
        HetznerAsg(hcloudToken).run(LoadBalancerReference.LoadBalancerName("hcloud-load-balancer-asg1"), 1, userData)
    }

    @Test
    fun testAsg2() {
        HetznerAsg(hcloudToken).run(LoadBalancerReference.LoadBalancerName("hcloud-load-balancer-asg2"), 1, userData)
    }

    @Test
    fun testAsg3() {
        HetznerAsg(hcloudToken).run(LoadBalancerReference.LoadBalancerName("hcloud-load-balancer-asg3"), 1, userData)
    }

}
