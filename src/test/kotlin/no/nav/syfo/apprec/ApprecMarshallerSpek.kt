package no.nav.syfo.apprec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.Apprec
import no.nav.syfo.serializeAppRec
import org.amshove.kluent.shouldContain
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ApprecMarshallerSpek : Spek({
    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val apprec: Apprec = objectMapper.readValue(Apprec::class.java.getResourceAsStream("/apprecOK.json").readBytes().toString(Charsets.UTF_8))

    describe("Serializing a apprec") {
        val apprecXML = createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf())
        val serializedApprec = serializeAppRec(apprecXML)

        it("Results in a XML without namespace prefixes") {
            serializedApprec shouldContain "<AppRec xmlns=\"http://www.kith.no/xmlstds/apprec/2004-11-21\">"
        }
    }
})
