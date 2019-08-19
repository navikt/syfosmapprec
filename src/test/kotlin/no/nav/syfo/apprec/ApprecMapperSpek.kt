package no.nav.syfo.apprec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.StringReader
import java.time.LocalDateTime
import javax.xml.bind.JAXBContext
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.syfo.Apprec
import no.nav.syfo.SyfoSmApprecConstant
import no.nav.syfo.get
import no.nav.syfo.serializeAppRec
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ApprecMapperSpek : Spek({
    val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val apprec: Apprec = objectMapper.readValue(Apprec::class.java.getResourceAsStream("/apprecOK.json").readBytes().toString(Charsets.UTF_8))

    val apprecUnmarshaller = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLAppRec::class.java, XMLMottakenhetBlokk::class.java)
            .createUnmarshaller()

    fun marshalAndUnmarshal(fellesformat: XMLEIFellesformat): XMLEIFellesformat =
            apprecUnmarshaller.unmarshal(StringReader(serializeAppRec(fellesformat))) as XMLEIFellesformat

    describe("Duplicate AppRec") {
        val apprecErrorDuplicate = createApprecError("Duplikat! - Denne sykmeldingen er mottatt tidligere. Skal ikke sendes på nytt.")
        val ff = marshalAndUnmarshal(createApprec(
                apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()))
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        it("Has the same ediLoggId as the source") {
            ff.get<XMLMottakenhetBlokk>().ediLoggId shouldEqual apprec.ediloggid
        }
        it("Sets appRec status dn to Avvist") {
            ff.get<XMLAppRec>().status.dn shouldEqual ApprecStatus.AVVIST.dn
        }
        it("Sets appRec error dn to duplicate") {
            ff.get<XMLAppRec>().error.first().dn shouldEqual apprecErrorDuplicate.dn
        }
        it("Sets appRec error v to duplicate") {
            ff.get<XMLAppRec>().error.first().v shouldEqual apprecErrorDuplicate.v
        }
        it("Sets appRec error s to duplicate") {
            ff.get<XMLAppRec>().error.first().s shouldEqual apprecErrorDuplicate.s
        }
    }

    describe("OK AppRec") {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        it("Sets ebRole to ebRoleNav") {
            ff.get<XMLMottakenhetBlokk>().ebRole shouldEqual SyfoSmApprecConstant.EBROLENAV.string
        }
        it("Sets ebService") {
            ff.get<XMLMottakenhetBlokk>().ebService shouldEqual SyfoSmApprecConstant.EBSERVICESYKMELDING.string
        }
        it("Sets ebAction") {
            ff.get<XMLMottakenhetBlokk>().ebAction shouldEqual SyfoSmApprecConstant.EBACTIONSVARMELDING.string
        }
        it("Sets appRec message type") {
            ff.get<XMLAppRec>().msgType.v shouldEqual SyfoSmApprecConstant.APPREC.string
        }
        it("Sets appRec miGversion") {
            ff.get<XMLAppRec>().miGversion shouldEqual SyfoSmApprecConstant.APPRECVERSIONV1_0.string
        }
        it("Sets genDate to current date") {
            val now = LocalDateTime.now()
            ff.get<XMLAppRec>().genDate.monthValue shouldEqual now.monthValue
            ff.get<XMLAppRec>().genDate.dayOfMonth shouldEqual now.dayOfMonth
            ff.get<XMLAppRec>().genDate.hour shouldEqual now.hour
        }
        it("Sets appRec id to ediLoggId") {
            ff.get<XMLAppRec>().id shouldEqual apprec.ediloggid
        }
        it("Sets senders appRec sender institution name to receiver organizationName") {
            ff.get<XMLAppRec>().sender.hcp.inst.name shouldEqual apprec.senderOrganisasjon.navn
        }
        it("Sets senders appRec institution id to first organization ident id") {
            ff.get<XMLAppRec>().sender.hcp.inst.id shouldEqual apprec.senderOrganisasjon.houvedIdent.id
        }
        it("Sets senders appRec institution typeId dn to first organization ident typeId dn") {
            ff.get<XMLAppRec>().sender.hcp.inst.typeId.dn shouldEqual
                    apprec.senderOrganisasjon.houvedIdent.typeId.beskrivelse
        }
        it("Sets senders appRec institution typeId v to first organization ident typeId v") {
            ff.get<XMLAppRec>().sender.hcp.inst.typeId.v shouldEqual
                    apprec.senderOrganisasjon.houvedIdent.typeId.verdi
        }
        it("Sets senders first additional appRec institution id to second organization ident id") {
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().id shouldEqual
                    apprec.senderOrganisasjon.tilleggsIdenter?.first()?.id
        }
        it("Sets senders first additional appRec institution typeId dn to second organization ident typeId dn") {
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.dn shouldEqual
                    apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.beskrivelse
        }
        it("Sets senders first additional appRec institution typeId v to second organization ident typeId v") {
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.v shouldEqual
                    apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.verdi
        }
        it("Sets receivers appRec institution name to sender organizationName") {
            ff.get<XMLAppRec>().receiver.hcp.inst.name shouldEqual
                    apprec.mottakerOrganisasjon.navn
        }
        it("Sets receivers appRec institution id to first sender organization ident id") {
            ff.get<XMLAppRec>().receiver.hcp.inst.id shouldEqual apprec.mottakerOrganisasjon.houvedIdent.id
        }
        it("Sets receivers appRec institution typeId dn to first sender organization ident typeId dn") {
            ff.get<XMLAppRec>().receiver.hcp.inst.typeId.dn shouldEqual
                    apprec.mottakerOrganisasjon.houvedIdent.typeId.beskrivelse
        }
        it("Sets receivers appRec institution typeId v to first organization ident typeId v") {
            ff.get<XMLAppRec>().receiver.hcp.inst.typeId.v shouldEqual
                    apprec.mottakerOrganisasjon.houvedIdent.typeId.verdi
        }

        it("Sets appRec status dn to OK") {
            ff.get<XMLAppRec>().status.dn shouldEqual ApprecStatus.OK.dn
        }
        it("Sets appRec status v to OK") {
            ff.get<XMLAppRec>().status.v shouldEqual ApprecStatus.OK.v
        }
        it("Sets appRec originalMsgId") {
            ff.get<XMLAppRec>().originalMsgId.msgType.dn shouldEqual "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding"
        }
        it("Sets appRec originalMsgId") {
            ff.get<XMLAppRec>().originalMsgId.msgType.v shouldEqual "SYKMELD"
        }
        it("Sets appRec genDate as issueDate") {
            ff.get<XMLAppRec>().originalMsgId.issueDate shouldEqual apprec.genDate
        }
        it("Sets appRec originalMsgId to msgId") {
            ff.get<XMLAppRec>().originalMsgId.id shouldEqual apprec.msgId
        }
    }
    describe("Error AppRec") {
        val apprecErrorinvalidFnrSize = createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()))
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)
        it("Sets appRec error dn to duplicate") {
            ff.get<XMLAppRec>().error.first().dn shouldEqual apprecErrorinvalidFnrSize.dn
        }
        it("Sets appRec error v to duplicate") {
            ff.get<XMLAppRec>().error.first().v shouldEqual apprecErrorinvalidFnrSize.v
        }
        it("Sets appRec error s to duplicate") {
            ff.get<XMLAppRec>().error.first().s shouldEqual apprecErrorinvalidFnrSize.s
        }
    }
})
