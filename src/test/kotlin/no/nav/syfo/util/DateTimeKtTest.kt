package no.nav.syfo.util

import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime
import java.time.OffsetDateTime

class DateTimeKtTest : Spek({
    describe("Test datetime formating") {
        it("Get LocalDateTime correct") {
            val localDateTime = LocalDateTime.parse("2021-03-03T12:01:01")
            getDateTimeString(localDateTime) shouldBeEqualTo "2021-03-03T12:01:01"
        }
        it("Get OffsetDateTime correct") {
            val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02+02:00")
            getDateTimeString(offsetDateTime) shouldBeEqualTo "2021-03-30T13:40:02+02:00"
        }
        it("Get OffsetDateTime correct UTC") {
            val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02Z")
            getDateTimeString(offsetDateTime) shouldBeEqualTo "2021-03-30T13:40:02Z"
        }
    }
})
