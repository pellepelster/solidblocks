package de.solidblocks.cloud.api

import io.vertx.ext.web.RoutingContext

fun RoutingContext.email() = this.user().principal().getString("email")
