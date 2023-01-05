package no.nav.syfo.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime

internal class DateTimeTest {
    @Test
    internal fun `Get LocalDateTime correct`() {
        val localDateTime = LocalDateTime.parse("2021-03-03T12:01:01")

        Assertions.assertEquals("2021-03-03T12:01:01", getDateTimeString(localDateTime))
    }

    @Test
    internal fun `Get OffsetDateTime correct`() {
        val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02+02:00")

        Assertions.assertEquals("2021-03-30T13:40:02+02:00", getDateTimeString(offsetDateTime))
    }

    @Test
    internal fun `Get OffsetDateTime correct UTC`() {
        val offsetDateTime = OffsetDateTime.parse("2021-03-30T13:40:02Z")
        Assertions.assertEquals("2021-03-30T13:40:02Z", getDateTimeString(offsetDateTime))
    }
}
