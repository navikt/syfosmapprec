package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.helse.apprecV1.XMLCV
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.apprec.ApprecStatus
import no.nav.syfo.apprec.createApprec
import no.nav.syfo.apprec.createApprecError
import no.nav.syfo.apprec.toApprecCV
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.metrics.APPREC_COUNTER
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.time.Duration
import java.util.Properties
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.Marshaller

private val log = LoggerFactory.getLogger("no.nav.syfo.smapprec")

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerKotlinModule()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val serviceUser = ServiceUser()
    MqTlsUtils.getMqTlsConfig().forEach { key, value -> System.setProperty(key as String, value as String) }
    val applicationState = ApplicationState()
    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)

    DefaultExports.initialize()

    val consumerAivenProperties = KafkaUtils.getAivenKafkaConfig().toConsumerConfig(
        "${env.applicationName}-consumer",
        StringDeserializer::class
    ).also {
        it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none"
        it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
    }

    launchListeners(
        applicationState,
        env,
        serviceUser,
        consumerAivenProperties
    )

    applicationServer.start()
}

@DelicateCoroutinesApi
fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: TrackableException) {
            log.error("En uhåndtert feil oppstod, applikasjonen restarter {}", fields(e.loggingMeta), e.cause)
        } finally {
            applicationState.ready = false
            applicationState.alive = false
        }
    }

@DelicateCoroutinesApi
fun launchListeners(
    applicationState: ApplicationState,
    env: Environment,
    serviceUser: ServiceUser,
    consumerAivenProperties: Properties
) {
    val kafkaAivenConsumerApprec = KafkaConsumer<String, String>(consumerAivenProperties)
    kafkaAivenConsumerApprec.subscribe(
        listOf(env.apprecTopic)
    )

    createListener(applicationState) {
        connectionFactory(env).createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword).use { connection ->
            connection.start()
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val kvitteringsProducer = session.producerForQueue(env.apprecQueueName)

            blockingApplicationLogic(applicationState, kvitteringsProducer, session, kafkaAivenConsumerApprec)
        }
    }
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    receiptProducer: MessageProducer,
    session: Session,
    kafkaAivenConsumer: KafkaConsumer<String, String>,
) {
    while (applicationState.ready) {
        kafkaAivenConsumer.poll(Duration.ofMillis(0)).forEach { consumerRecord ->
            val apprec: Apprec = objectMapper.readValue(consumerRecord.value())

            val loggingMeta = LoggingMeta(
                mottakId = apprec.ediloggid,
                msgId = apprec.msgId
            )

            handleMessage(apprec, receiptProducer, session, loggingMeta, "aiven")
        }
        delay(100)
    }
}

suspend fun handleMessage(
    apprec: Apprec,
    receiptProducer: MessageProducer,
    session: Session,
    loggingMeta: LoggingMeta,
    source: String
) {
    wrapExceptions(loggingMeta) {
        log.info("Received a SM2013 from $source, {}", fields(loggingMeta))
        if (apprec.apprecStatus == ApprecStatus.AVVIST) {
            if (apprec.validationResult != null) {
                sendReceipt(
                    session, receiptProducer, apprec, ApprecStatus.AVVIST, loggingMeta,
                    apprec.validationResult.ruleHits.map { it.toApprecCV() }
                )
            } else {
                sendReceipt(
                    session, receiptProducer, apprec, ApprecStatus.AVVIST,
                    loggingMeta, listOf(createApprecError(apprec.tekstTilSykmelder))
                )
            }
        } else {
            sendReceipt(session, receiptProducer, apprec, ApprecStatus.OK, loggingMeta)
        }
    }
}

fun sendReceipt(
    session: Session,
    receiptProducer: MessageProducer,
    apprec: Apprec,
    apprecStatus: ApprecStatus,
    loggingMeta: LoggingMeta,
    apprecErrors: List<XMLCV> = listOf()
) {
    val ediloggid = apprec.ediloggid

    APPREC_COUNTER.inc()
    receiptProducer.send(
        session.createTextMessage().apply {
            val apprecFellesformat = createApprec(ediloggid, apprec, apprecStatus, apprecErrors)
            text = serializeAppRec(apprecFellesformat)
        }
    )
    log.info("Apprec sendt til emottak, {}", fields(loggingMeta))
}

fun serializeAppRec(fellesformat: XMLEIFellesformat) = apprecFFJaxbMarshaller.toString(fellesformat)

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

inline fun <reified T> XMLEIFellesformat.get(): T = any.find { it is T } as T
