package de.soldiblocks.ingress.api

import de.solidblocks.base.reference.ServiceReference

data class ServiceIngressRequest(val reference: ServiceReference, val hostnames: List<String>, val upstream: String)
