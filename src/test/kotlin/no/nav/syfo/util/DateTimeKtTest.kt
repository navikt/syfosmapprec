package no.nav.syfo.util

import io.kotest.core.spec.style.FunSpec
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDateTime
import java.time.OffsetDateTime

class DateTimeKtTest : FunSpec({
    context("Test datetime formating") {
        test("Get LocalDateTime correct") {
            val localDateTime = LocalDateTime.parse("2021-03-03T12:01:01")
            getDateTimeString(localDateTime) shouldBeEqualTo "2021-03-03T12:01:01"
        }
        test("Get OffsetDateTime correct") {
            val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02+02:00")
            getDateTimeString(offsetDateTime) shouldBeEqualTo "2021-03-30T13:40:02+02:00"
        }
        test("Get OffsetDateTime correct UTC") {
            val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02Z")
            getDateTimeString(offsetDateTime) shouldBeEqualTo "2021-03-30T13:40:02Z"
        }
    }
})
