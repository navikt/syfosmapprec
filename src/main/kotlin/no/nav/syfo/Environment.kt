package no.nav.syfo

import no.nav.syfo.mq.MqConfig
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

data class Environment(
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "syfosmapprec"),
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    override val mqHostname: String = getEnvVar("MQ_HOST_NAME"),
    override val mqPort: Int = getEnvVar("MQ_PORT").toInt(),
    override val mqGatewayName: String = getEnvVar("MQ_GATEWAY_NAME"),
    override val mqChannelName: String = getEnvVar("MQ_CHANNEL_NAME"),
    val apprecQueueName: String = getEnvVar("MQ_APPREC_QUEUE_NAME"),
    val apprecTopic: String = "teamsykmelding.sykmelding-apprec"
) : MqConfig

data class VaultServiceUser(
    val serviceuserUsername: String = getFileAsString("/secrets/serviceuser/username"),
    val serviceuserPassword: String = getFileAsString("/secrets/serviceuser/password")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun getFileAsString(filePath: String) = String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
