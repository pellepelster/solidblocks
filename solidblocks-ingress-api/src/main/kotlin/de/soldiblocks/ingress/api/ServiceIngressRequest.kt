package de.soldiblocks.ingress.api

import de.solidblocks.base.resources.ServiceResource

data class ServiceIngressRequest(val reference: ServiceResource, val hostnames: List<String>, val upstream: String)
