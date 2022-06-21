package no.nav.syfo

import no.nav.syfo.mq.MqConfig

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
    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
