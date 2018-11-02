package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smapprec")

fun Routing.registerApprecApi() {
    post("/v1/apprec") {
        log.info("Got an request to send apprec message")

        call.respond(mapOf("OK" to true))
    }
}
