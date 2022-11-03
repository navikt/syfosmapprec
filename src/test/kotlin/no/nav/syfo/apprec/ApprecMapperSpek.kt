package no.nav.syfo.apprec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.syfo.Apprec
import no.nav.syfo.SyfoSmApprecConstant
import no.nav.syfo.get
import no.nav.syfo.serializeAppRec
import no.nav.syfo.util.getDateTimeString
import org.amshove.kluent.shouldBeEqualTo
import java.io.StringReader
import java.time.LocalDateTime
import javax.xml.bind.JAXBContext

class ApprecMapperSpek : FunSpec({
    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val apprec: Apprec = objectMapper.readValue(Apprec::class.java.getResourceAsStream("/apprecOK.json").readBytes().toString(Charsets.UTF_8))

    val apprecUnmarshaller = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLAppRec::class.java, XMLMottakenhetBlokk::class.java)
        .createUnmarshaller()

    fun marshalAndUnmarshal(fellesformat: XMLEIFellesformat): XMLEIFellesformat =
        apprecUnmarshaller.unmarshal(StringReader(serializeAppRec(fellesformat))) as XMLEIFellesformat

    context("Duplicate AppRec") {
        val apprecErrorDuplicate = createApprecError("Duplikat! - Denne sykmeldingen er mottatt tidligere. Skal ikke sendes på nytt.")
        val ff = marshalAndUnmarshal(
            createApprec(
                apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()
            )
        )
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        test("Has the same ediLoggId as the source") {
            ff.get<XMLMottakenhetBlokk>().ediLoggId shouldBeEqualTo apprec.ediloggid
        }
        test("Sets appRec status dn to Avvist") {
            ff.get<XMLAppRec>().status.dn shouldBeEqualTo ApprecStatus.AVVIST.dn
        }
        test("Sets appRec error dn to duplicate") {
            ff.get<XMLAppRec>().error.first().dn shouldBeEqualTo apprecErrorDuplicate.dn
        }
        test("Sets appRec error v to duplicate") {
            ff.get<XMLAppRec>().error.first().v shouldBeEqualTo apprecErrorDuplicate.v
        }
        test("Sets appRec error s to duplicate") {
            ff.get<XMLAppRec>().error.first().s shouldBeEqualTo apprecErrorDuplicate.s
        }
    }

    context("OK AppRec") {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        test("should get correct genDate") {
            val apprecWithMsgGenDate = marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec.copy(
                        genDate = LocalDateTime.parse("2021-03-03T12:02:02"),
                        msgGenDate = "2021-03-03T12:01:01+01:00"
                    ),
                    ApprecStatus.OK,
                    listOf()
                )
            )
            apprecWithMsgGenDate.get<XMLAppRec>().originalMsgId.issueDate shouldBeEqualTo "2021-03-03T12:01:01+01:00"
        }
        test("should get correct genDate with msgGenDate = null") {
            val apprecWithoutMsgGenDate = marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec.copy(genDate = LocalDateTime.parse("2021-03-03T12:02:02")),
                    ApprecStatus.OK,
                    listOf()
                )
            )
            apprecWithoutMsgGenDate.get<XMLAppRec>().originalMsgId.issueDate shouldBeEqualTo "2021-03-03T12:02:02"
        }
        test("Sets ebRole to ebRoleNav") {
            ff.get<XMLMottakenhetBlokk>().ebRole shouldBeEqualTo SyfoSmApprecConstant.EBROLENAV.string
        }
        test("Sets ebService") {
            ff.get<XMLMottakenhetBlokk>().ebService shouldBeEqualTo "Sykmelding"
        }
        test("Sets ebAction") {
            ff.get<XMLMottakenhetBlokk>().ebAction shouldBeEqualTo SyfoSmApprecConstant.EBACTIONSVARMELDING.string
        }
        test("Sets appRec message type") {
            ff.get<XMLAppRec>().msgType.v shouldBeEqualTo SyfoSmApprecConstant.APPREC.string
        }
        test("Sets appRec miGversion") {
            ff.get<XMLAppRec>().miGversion shouldBeEqualTo SyfoSmApprecConstant.APPRECVERSIONV1_0.string
        }
        test("Sets appRec id to ediLoggId") {
            ff.get<XMLAppRec>().id shouldBeEqualTo apprec.ediloggid
        }
        test("Sets senders appRec sender institution name to receiver organizationName") {
            ff.get<XMLAppRec>().sender.hcp.inst.name shouldBeEqualTo apprec.senderOrganisasjon.navn
        }
        test("Sets senders appRec institution id to first organization ident id") {
            ff.get<XMLAppRec>().sender.hcp.inst.id shouldBeEqualTo apprec.senderOrganisasjon.hovedIdent.id
        }
        test("Sets senders appRec institution typeId dn to first organization ident typeId dn") {
            ff.get<XMLAppRec>().sender.hcp.inst.typeId.dn shouldBeEqualTo
                apprec.senderOrganisasjon.hovedIdent.typeId.beskrivelse
        }
        test("Sets senders appRec institution typeId v to first organization ident typeId v") {
            ff.get<XMLAppRec>().sender.hcp.inst.typeId.v shouldBeEqualTo
                apprec.senderOrganisasjon.hovedIdent.typeId.verdi
        }
        test("Sets senders first additional appRec institution id to second organization ident id") {
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().id shouldBeEqualTo
                apprec.senderOrganisasjon.tilleggsIdenter?.first()?.id
        }
        test("Sets senders first additional appRec institution typeId dn to second organization ident typeId dn") {
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.dn shouldBeEqualTo
                apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.beskrivelse
        }
        test("Sets senders first additional appRec institution typeId v to second organization ident typeId v") {
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.v shouldBeEqualTo
                apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.verdi
        }
        test("Sets receivers appRec institution name to sender organizationName") {
            ff.get<XMLAppRec>().receiver.hcp.inst.name shouldBeEqualTo
                apprec.mottakerOrganisasjon.navn
        }
        test("Sets receivers appRec institution id to first sender organization ident id") {
            ff.get<XMLAppRec>().receiver.hcp.inst.id shouldBeEqualTo apprec.mottakerOrganisasjon.hovedIdent.id
        }
        test("Sets receivers appRec institution typeId dn to first sender organization ident typeId dn") {
            ff.get<XMLAppRec>().receiver.hcp.inst.typeId.dn shouldBeEqualTo
                apprec.mottakerOrganisasjon.hovedIdent.typeId.beskrivelse
        }
        test("Sets receivers appRec institution typeId v to first organization ident typeId v") {
            ff.get<XMLAppRec>().receiver.hcp.inst.typeId.v shouldBeEqualTo
                apprec.mottakerOrganisasjon.hovedIdent.typeId.verdi
        }

        test("Sets receivers appRec id to first organization ident id") {
            ff.get<XMLAppRec>().receiver.hcp.inst.hcPerson.first().id shouldBeEqualTo
                apprec.mottakerOrganisasjon.helsepersonell?.hovedIdent?.id
        }

        test("Sets appRec status dn to OK") {
            ff.get<XMLAppRec>().status.dn shouldBeEqualTo ApprecStatus.OK.dn
        }
        test("Sets appRec status v to OK") {
            ff.get<XMLAppRec>().status.v shouldBeEqualTo ApprecStatus.OK.v
        }
        test("Sets appRec originalMsgId") {
            ff.get<XMLAppRec>().originalMsgId.msgType.dn shouldBeEqualTo "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding"
        }
        test("Sets appRec originalMsgId") {
            ff.get<XMLAppRec>().originalMsgId.msgType.v shouldBeEqualTo "SYKMELD"
        }
        test("Sets appRec genDate as issueDate") {
            ff.get<XMLAppRec>().originalMsgId.issueDate shouldBeEqualTo getDateTimeString(apprec.genDate)
        }
        test("Sets appRec originalMsgId to msgid") {
            ff.get<XMLAppRec>().originalMsgId.id shouldBeEqualTo apprec.msgId
        }
    }
    context("Error AppRec") {
        val apprecErrorinvalidFnrSize = createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()))
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)
        test("Sets appRec error dn to duplicate") {
            ff.get<XMLAppRec>().error.first().dn shouldBeEqualTo apprecErrorinvalidFnrSize.dn
        }
        test("Sets appRec error v to duplicate") {
            ff.get<XMLAppRec>().error.first().v shouldBeEqualTo apprecErrorinvalidFnrSize.v
        }
        test("Sets appRec error s to duplicate") {
            ff.get<XMLAppRec>().error.first().s shouldBeEqualTo apprecErrorinvalidFnrSize.s
        }
    }
})
