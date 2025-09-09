package de.solidblocks.cli

import de.solidblocks.cli.hetzner.asg.HetznerAsg
import de.solidblocks.cli.hetzner.asg.LoadBalancerReference
import org.junit.jupiter.api.Test

class HetznerAsgTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()

    @Test
    fun testAsg1() {
        HetznerAsg(hcloudToken).run(LoadBalancerReference.LoadBalancerName("hcloud-load-balancer-asg1"))
    }

    @Test
    fun testAsg2() {
        HetznerAsg(hcloudToken).run(LoadBalancerReference.LoadBalancerName("hcloud-load-balancer-asg2"))
    }

    @Test
    fun testAsg3() {
        HetznerAsg(hcloudToken).run(LoadBalancerReference.LoadBalancerName("hcloud-load-balancer-asg3"))
    }

}
