package no.nav.syfo.apprec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.Apprec
import no.nav.syfo.serializeAppRec
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

internal class ApprecMarshallerTest {
    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val apprec: Apprec = objectMapper.readValue(
        Apprec::class.java.getResourceAsStream("/apprecOK.json")!!.readBytes().toString(Charsets.UTF_8)
    )

    @Test
    internal fun `Serializing a apprec results in a XML without namespace prefixes`() {
        val apprecXML = createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf())
        val serializedApprec = serializeAppRec(apprecXML)
        serializedApprec shouldContain "<AppRec xmlns=\"http://www.kith.no/xmlstds/apprec/2004-11-21\">"
    }
}
