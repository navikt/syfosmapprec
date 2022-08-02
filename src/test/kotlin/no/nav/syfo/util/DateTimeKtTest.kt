package no.nav.syfo.util

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime

internal class DateTimeKtTest {

    @Test
    internal fun `Get LocalDateTime correct`() {
        val localDateTime = LocalDateTime.parse("2021-03-03T12:01:01")
        getDateTimeString(localDateTime) shouldBeEqualTo "2021-03-03T12:01:01"
    }

    @Test
    internal fun `Get OffsetDateTime correct`() {
        val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02+02:00")
        getDateTimeString(offsetDateTime) shouldBeEqualTo "2021-03-30T13:40:02+02:00"
    }

    @Test
    internal fun `Get OffsetDateTime correct UTC`() {
        val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02Z")
        getDateTimeString(offsetDateTime) shouldBeEqualTo "2021-03-30T13:40:02Z"
    }
}
