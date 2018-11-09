package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.kith.xmlstds.apprec._2004_11_21.XMLAppRec
import no.nav.helse.sm2013.EIFellesformat
import no.nav.syfo.Environment
import no.nav.syfo.apprec.createApprec
import no.nav.syfo.apprec.mapApprecErrorToAppRecCV
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

data class ApprecStatus(
    val v: String,
    val dn: String
)

data class ApprecError(
    val v: String,
    val dn: String,
    val s: String
)

fun Routing.registerApprecApi(env: Environment) {
    post("/v1/apprec") {
        log.info("Got an request to send apprec message")

        connectionFactory(env).createConnection(env.mqUsername, env.mqPassword).use { connection ->
            connection.start()
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val receiptQueue = session.createQueue(env.apprecQueue)
            val receiptProducer = session.createProducer(receiptQueue)
            val fellesformat = fellesformatUnmarshaller.unmarshal(call.receiveStream()) as EIFellesformat
            val apprecStatus = ApprecStatus("2", "Avvist")
            val apprecError = ApprecError("54", "Duplikat! - Denne legeerklæringen meldingen er mottatt tidligere. Skal ikke sendes på nytt.",
                    "2.16.578.1.12.4.1.1.8222")

            sendReceipt(session, receiptProducer, fellesformat, apprecStatus, apprecError)

            call.respond(OK, true)
    }
    }
}

fun sendReceipt(
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: EIFellesformat,
    apprecStatus: ApprecStatus,
    vararg apprecErrors: ApprecError
) {
    receiptProducer.send(session.createTextMessage().apply {
        val xmleiFellesformat = createApprec(fellesformat, apprecStatus)
        xmleiFellesformat.get<XMLAppRec>().error.addAll(apprecErrors.map { mapApprecErrorToAppRecCV(it) })
        text = apprecMarshaller.toString(xmleiFellesformat)
    })
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T

inline fun <reified T> EIFellesformat.get(): T = any.find { it is T } as T

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}