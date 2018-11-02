package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "1").toInt(),
    val srvappnameUsername: String = getEnvVar("SRVSYFOSMAPPREC_USERNAME"),
    val srvappnamePassword: String = getEnvVar("SRVSYFOSMAPPREC_PASSWORD")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
