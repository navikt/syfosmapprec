package no.nav.syfo.apprec

import no.nav.syfo.serializeAppRec
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

internal class ApprecMarshallerTest {
    private val jsonMapper: JsonMapper = jacksonMapperBuilder().build()

    private val apprec: Apprec =
        jsonMapper.readValue(
            Apprec::class
                .java
                .getResourceAsStream("/apprecOK.json")!!
                .readBytes()
                .toString(Charsets.UTF_8)
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
