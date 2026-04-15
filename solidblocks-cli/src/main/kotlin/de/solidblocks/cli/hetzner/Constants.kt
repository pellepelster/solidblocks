package de.solidblocks.cli.hetzner

import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.pascalCaseToWhiteSpace
import kotlin.reflect.KClass

object Constants {
    const val NAMESPACE_LABEL = "blcks.de"

    const val USER_DATA_HASH_LABEL = "${NAMESPACE_LABEL}/user-data-hash"

    const val MANAGED_BY_LABEL = "${NAMESPACE_LABEL}/managed-by"

    const val TEST_LABEL = "${NAMESPACE_LABEL}/test"

    const val LOADBALANCER_ID_LABEL = "${NAMESPACE_LABEL}/load-balancer-id"

    const val DEPLOYMENT_ID_LABEL = "${NAMESPACE_LABEL}/deployment-id"
}

fun HetznerNamedResource<*>.logText() = "${this::class.pascalCaseToWhiteSpace()} '${name ?: "<no name>"}' ($id)"

fun KClass<out HetznerNamedResource<*>>.pascalCaseToWhiteSpace() = this.simpleName!!.removeSuffix("Response").pascalCaseToWhiteSpace().lowercase()
