package no.nav.syfo.util

import java.time.LocalDateTime
import java.time.OffsetDateTime

fun getDateTimeString(localDateTime: LocalDateTime): String {
    return localDateTime.toString()
}

fun getDateTimeString(timestamp: OffsetDateTime): String {
    return timestamp.toString()
}
