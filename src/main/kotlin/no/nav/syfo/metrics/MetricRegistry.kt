package no.nav.syfo.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "syfosmapprec"

val APPREC_COUNTER: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name("apprec_count")
    .help("Counts the number of apprec messages")
    .register()
