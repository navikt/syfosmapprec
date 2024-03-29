package no.nav.syfo.apprec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.serializeAppRec
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ApprecMarshallerTest {
    private val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val apprec: Apprec =
        objectMapper.readValue(
            Apprec::class
                .java
                .getResourceAsStream("/apprecOK.json")!!
                .readBytes()
                .toString(Charsets.UTF_8),
        )

    @Test
    internal fun `Serializing a apprec results in a XML without namespace prefixes`() {
        val apprecXML = createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf())
        val serializedApprec = serializeAppRec(apprecXML)

        Assertions.assertEquals(
            true,
            serializedApprec.contains(
                "<AppRec xmlns=\"http://www.kith.no/xmlstds/apprec/2004-11-21\">"
            ),
        )
    }
}
