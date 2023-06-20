package no.nav.syfo.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "syfosmapprec"

val APPREC_COUNTER: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("apprec_count")
        .help("Counts the number of apprec messages")
        .register()

val APPREC_INVALID: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("apprec_invalid")
        .help("Counts the number of invalid apprec messages")
        .register()

val APPREC_OK: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("apprec_ok")
        .help("Counts the number of ok apprec messages")
        .register()
