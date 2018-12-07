package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "1").toInt(),
    val srvappnameUsername: String = getEnvVar("SRVSYFOSMAPPREC_USERNAME", "username"),
    val srvappnamePassword: String = getEnvVar("SRVSYFOSMAPPREC_PASSWORD", "password"),
    val mqHostname: String = getEnvVar("MQGATEWAY03_HOSTNAME"),
    val mqPort: Int = getEnvVar("MQGATEWAY03_PORT").toInt(),
    val mqGatewayName: String = getEnvVar("MQGATEWAY03_NAME"),
    val mqChannelName: String = getEnvVar("SYFOSMAPPREC_CHANNEL_NAME", "Q1_SYFOSMAPPREC"),
    val mqUsername: String = getEnvVar("SRVAPPSERVER_USERNAME", "srvappserver"),
    val mqPassword: String = getEnvVar("SRVAPPSERVER_PASSWORD", ""),
    val apprecQueue: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
