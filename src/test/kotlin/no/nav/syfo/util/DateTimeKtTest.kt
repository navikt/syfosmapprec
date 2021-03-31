package no.nav.syfo.util

import java.time.LocalDateTime
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DateTimeKtTest : Spek({
    describe("Test datetime formating") {
        it("Get LocalDateTime correct") {
            val localDateTime = LocalDateTime.parse("2021-03-03T12:01:01")
            getDateTimeString(localDateTime) shouldEqual "2021-03-03T12:01:01"
        }
        it("Get OffsetDateTime correct") {
            val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02+02:00")
            getDateTimeString(offsetDateTime) shouldEqual "2021-03-30T13:40:02+02:00"
        }
        it("Get OffsetDateTime correct UTC") {
            val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02Z")
            getDateTimeString(offsetDateTime) shouldEqual "2021-03-30T13:40:02Z"
        }
    }
})
