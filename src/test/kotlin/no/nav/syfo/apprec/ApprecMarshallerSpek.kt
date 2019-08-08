package no.nav.syfo.apprec

import java.io.StringReader
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.syfo.fellesformatUnmarshaller
import no.nav.syfo.serializeAppRec
import no.nav.syfo.utils.getFileAsString
import org.amshove.kluent.shouldContain
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ApprecMarshallerSpek : Spek({
    describe("Serializing a apprec") {
        val stringInput = getFileAsString("src/test/resources/sykemelding2013Regelsettversjon2.xml")
        val fellesformat = fellesformatUnmarshaller.unmarshal(StringReader(stringInput)) as XMLEIFellesformat
        val apprec = createApprec(fellesformat, ApprecStatus.OK, listOf())
        val serializedApprec = serializeAppRec(apprec)

        it("Results in a XML without namespace prefixes") {
            serializedApprec shouldContain "<AppRec xmlns=\"http://www.kith.no/xmlstds/apprec/2004-11-21\">"
        }
    }
})
