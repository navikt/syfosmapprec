package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.time.Duration
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.Marshaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.helse.apprecV1.XMLCV
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.apprec.ApprecStatus
import no.nav.syfo.apprec.createApprec
import no.nav.syfo.apprec.createApprecError
import no.nav.syfo.apprec.toApprecCV
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.metrics.APPREC_COUNTER
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

data class ApplicationState(var running: Boolean = true, var ready: Boolean = false)

private val log = LoggerFactory.getLogger("no.nav.syfosmapprec")

val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val coroutineContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

@KtorExperimentalAPI
fun main() = runBlocking(coroutineContext) {
    val env = Environment()
    val credentials = objectMapper.readValue<VaultCredentials>(File("/var/run/secrets/nais.io/vault/credentials.json"))
    val applicationState = ApplicationState()

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState)
    }.start(wait = false)

    DefaultExports.initialize()

    connectionFactory(env).createConnection(credentials.mqUsername, credentials.mqPassword).use { connection ->
        connection.start()

        val kafkaBaseConfig = loadBaseConfig(env, credentials)
        val consumerProperties = kafkaBaseConfig.toConsumerConfig("${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class)

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val kvitteringsProducer = session.producerForQueue(env.apprecQueueName)

        launchListeners(
                applicationState,
                kvitteringsProducer,
                session,
                env,
                consumerProperties)

        Runtime.getRuntime().addShutdownHook(Thread {
            applicationServer.stop(10, 10, TimeUnit.SECONDS)
        })
    }
}

fun CoroutineScope.createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
        launch {
            try {
                action()
            } catch (e: TrackableException) {
                log.error("En uhåndtert feil oppstod, applikasjonen restarter {}", StructuredArguments.fields(e.loggingMeta), e.cause)
            } finally {
                applicationState.running = false
            }
        }

@KtorExperimentalAPI
suspend fun CoroutineScope.launchListeners(
    applicationState: ApplicationState,
    receiptProducer: MessageProducer,
    session: Session,
    env: Environment,
    consumerProperties: Properties
) {
    val recievedSykmeldingListeners = 0.until(env.applicationThreads).map {
        val kafkaconsumerRecievedSykmelding = KafkaConsumer<String, String>(consumerProperties)

        kafkaconsumerRecievedSykmelding.subscribe(
                listOf(env.sm2013Apprec)
        )
        createListener(applicationState) {
            blockingApplicationLogic(applicationState, kafkaconsumerRecievedSykmelding, receiptProducer, session)
        }
    }.toList()

    applicationState.ready = true
    recievedSykmeldingListeners.forEach { it.join() }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    kafkaConsumer: KafkaConsumer<String, String>,
    receiptProducer: MessageProducer,
    session: Session
) {
    while (applicationState.running) {
        kafkaConsumer.poll(Duration.ofMillis(0)).forEach { consumerRecord ->
            val apprec: Apprec = objectMapper.readValue(consumerRecord.value())
            val fellesformat = fellesformatUnmarshaller.unmarshal(StringReader(apprec.fellesformat)) as XMLEIFellesformat

            val receiverBlock = fellesformat.get<XMLMottakenhetBlokk>()
            val msgHead = fellesformat.get<XMLMsgHead>()

            val loggingMeta = LoggingMeta(
                    mottakId = receiverBlock.ediLoggId,
                    orgNr = hentUtSykmeldersOrganisasjonsNummer(fellesformat)?.id,
                    msgId = msgHead.msgInfo.msgId
            )

            handleMessage(apprec, receiptProducer, session, loggingMeta, fellesformat)
        }
        delay(100)
    }
}

@KtorExperimentalAPI
suspend fun handleMessage(
    apprec: Apprec,
    receiptProducer: MessageProducer,
    session: Session,
    loggingMeta: LoggingMeta,
    fellesformat: XMLEIFellesformat
) = coroutineScope {
    wrapExceptions(loggingMeta) {
        log.info("Received a SM2013, {}", fields(loggingMeta))
        if (apprec.apprecStatus == ApprecStatus.AVVIST) {
            if (apprec.validationResult != null) {
                sendReceipt(session, receiptProducer, fellesformat, ApprecStatus.AVVIST, loggingMeta, apprec.validationResult.ruleHits.map { it.toApprecCV() })
            } else {
                sendReceipt(session, receiptProducer, fellesformat, apprec.apprecStatus, loggingMeta, listOf(createApprecError(apprec.tekstTilSykmelder)))
            }
        } else {
            sendReceipt(session, receiptProducer, fellesformat, apprec.apprecStatus, loggingMeta)
        }
    }
}

fun sendReceipt(
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: XMLEIFellesformat,
    apprecStatus: ApprecStatus,
    loggingMeta: LoggingMeta,
    apprecErrors: List<XMLCV> = listOf()
) {
    APPREC_COUNTER.inc()
    receiptProducer.send(session.createTextMessage().apply {
        val apprec = createApprec(fellesformat, apprecStatus, apprecErrors)
        text = serializeAppRec(apprec)
    })
    log.info("Apprec sendt til emottak, {}", fields(loggingMeta))
}

fun serializeAppRec(fellesformat: XMLEIFellesformat) = apprecFFJaxbMarshaller.toString(fellesformat)

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

fun Application.initRouting(applicationState: ApplicationState) {
    routing {
        registerNaisApi(readynessCheck = { applicationState.ready }, livenessCheck = { applicationState.running })
    }
}

inline fun <reified T> XMLEIFellesformat.get(): T = any.find { it is T } as T

fun hentUtSykmeldersOrganisasjonsNummer(fellesformat: XMLEIFellesformat): XMLIdent? =
        fellesformat.get<XMLMsgHead>().msgInfo.sender.organisation.ident.find {
            it.typeId.v == "ENH"
        }
