package no.nav.syfo.util

import java.time.LocalDateTime
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class DateTimeKtTest : Spek({
    describe("Test datetime formating") {
        val localDateTime = LocalDateTime.parse("2021-03-03T12:01:01")
        getDateTimeString(localDateTime) shouldEqual "2021-03-03T12:01:01"
    }
})
