package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.helse.sm2013.EIFellesformat
import no.nav.syfo.Environment
import no.nav.syfo.apprecMarshaller
import no.nav.syfo.fellesformatUnmarshaller
import no.nav.syfo.util.connectionFactory
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringWriter
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.Marshaller

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smapprec")

fun Routing.registerApprecApi(env: Environment) {
    post("/v1/apprec") {
        log.info("Got an request to send apprec message")

        connectionFactory(env).createConnection(env.mqUsername, env.mqPassword).use { connection ->
            connection.start()
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val receiptQueue = session.createQueue(env.apprecQueue)
            val receiptProducer = session.createProducer(receiptQueue)
            val fellesformat = fellesformatUnmarshaller.unmarshal(call.receiveStream()) as EIFellesformat

            sendReceipt(session, receiptProducer, fellesformat)

            call.respond(OK, true)
    }
    }
}

fun sendReceipt(
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: EIFellesformat
) {
    receiptProducer.send(session.createTextMessage().apply {
        text = apprecMarshaller.toString(fellesformat)
    })
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T

inline fun <reified T> EIFellesformat.get(): T = any.find { it is T } as T

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}