package no.nav.syfo.apprec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.syfo.Apprec
import no.nav.syfo.SyfoSmApprecConstant
import no.nav.syfo.get
import no.nav.syfo.serializeAppRec
import no.nav.syfo.util.getDateTimeString
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.time.LocalDateTime
import javax.xml.bind.JAXBContext

internal class ApprecMapperTest {
    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val apprec: Apprec = objectMapper.readValue(
        Apprec::class.java.getResourceAsStream("/apprecOK.json")!!.readBytes().toString(Charsets.UTF_8)
    )

    val apprecUnmarshaller =
        JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLAppRec::class.java, XMLMottakenhetBlokk::class.java)
            .createUnmarshaller()

    fun marshalAndUnmarshal(fellesformat: XMLEIFellesformat): XMLEIFellesformat =
        apprecUnmarshaller.unmarshal(StringReader(serializeAppRec(fellesformat))) as XMLEIFellesformat

    private val apprecErrorDuplicate =
        createApprecError("Duplikat! - Denne sykmeldingen er mottatt tidligere. Skal ikke sendes på nytt.")
    val ff = marshalAndUnmarshal(
        createApprec(
            apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()
        )
    )

    @Test
    internal fun `Duplicate AppRec has the same ediLoggId as the source`() {
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        ff.get<XMLMottakenhetBlokk>().ediLoggId shouldBeEqualTo apprec.ediloggid
    }

    @Test
    internal fun `Duplicate AppRec sets appRec status dn to Avvist`() {
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        ff.get<XMLAppRec>().status.dn shouldBeEqualTo ApprecStatus.AVVIST.dn
    }

    @Test
    internal fun `Duplicate AppRec sets appRec error dn to duplicate`() {
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        ff.get<XMLAppRec>().error.first().dn shouldBeEqualTo apprecErrorDuplicate.dn
    }

    @Test
    internal fun `Duplicate AppRec sets appRec error v to duplicate`() {
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        ff.get<XMLAppRec>().error.first().v shouldBeEqualTo apprecErrorDuplicate.v
    }

    @Test
    internal fun `Duplicate AppRec sets appRec error s to duplicate`() {
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        ff.get<XMLAppRec>().error.first().s shouldBeEqualTo apprecErrorDuplicate.s
    }

    @Test
    internal fun `OK AppRec should get correct genDate`() {
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

    @Test
    internal fun `OK AppRec should get correct genDate with msgGenDate = null`() {
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

    @Test
    internal fun `OK AppRec sets ebRole to ebRoleNav`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLMottakenhetBlokk>().ebRole shouldBeEqualTo SyfoSmApprecConstant.EBROLENAV.string
    }

    @Test
    internal fun `OK AppRec sets ebService`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLMottakenhetBlokk>().ebRole shouldBeEqualTo SyfoSmApprecConstant.EBROLENAV.string
    }

    @Test
    internal fun `OK AppRec sets ebAction`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLMottakenhetBlokk>().ebAction shouldBeEqualTo SyfoSmApprecConstant.EBACTIONSVARMELDING.string
    }

    @Test
    internal fun `OK AppRec sets appRec message type`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().msgType.v shouldBeEqualTo SyfoSmApprecConstant.APPREC.string
    }

    @Test
    internal fun `OK AppRec sets appRec miGversione`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().miGversion shouldBeEqualTo SyfoSmApprecConstant.APPRECVERSIONV1_0.string
    }

    @Test
    internal fun `OK AppRec sets appRec id to ediLoggId`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().id shouldBeEqualTo apprec.ediloggid
    }

    @Test
    internal fun `OK AppRec sets senders appRec sender institution name to receiver organizationName`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().sender.hcp.inst.name shouldBeEqualTo apprec.senderOrganisasjon.navn
    }

    @Test
    internal fun `OK AppRec sets senders appRec institution id to first organization ident id`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().sender.hcp.inst.id shouldBeEqualTo apprec.senderOrganisasjon.hovedIdent.id
    }

    @Test
    internal fun `OK AppRec sets senders appRec institution typeId dn to first organization ident typeId dn`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().sender.hcp.inst.typeId.dn shouldBeEqualTo
            apprec.senderOrganisasjon.hovedIdent.typeId.beskrivelse
    }

    @Test
    internal fun `OK AppRec sets senders appRec institution typeId v to first organization ident typeId v`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().sender.hcp.inst.typeId.v shouldBeEqualTo
            apprec.senderOrganisasjon.hovedIdent.typeId.verdi
    }

    @Test
    internal fun `OK AppRec sets senders first additional appRec institution id to second organization ident id`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().id shouldBeEqualTo
            apprec.senderOrganisasjon.tilleggsIdenter?.first()?.id
    }

    @Test
    internal fun `OK AppRec sets senders first additional appRec institution typeId dn to second organization ident typeId dn`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.dn shouldBeEqualTo
            apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.beskrivelse
    }

    @Test
    internal fun `OK AppRec sets senders first additional appRec institution typeId v to second organization ident typeId v`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.v shouldBeEqualTo
            apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.verdi
    }

    @Test
    internal fun `OK AppRec sets receivers appRec institution name to sender organizationName`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().receiver.hcp.inst.name shouldBeEqualTo
            apprec.mottakerOrganisasjon.navn
    }

    @Test
    internal fun `OK AppRec sets receivers appRec institution id to first sender organization ident id`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().receiver.hcp.inst.id shouldBeEqualTo apprec.mottakerOrganisasjon.hovedIdent.id
    }

    @Test
    internal fun `OK AppRec sets receivers appRec institution typeId dn to first sender organization ident typeId dn`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().receiver.hcp.inst.typeId.dn shouldBeEqualTo
            apprec.mottakerOrganisasjon.hovedIdent.typeId.beskrivelse
    }

    @Test
    internal fun `OK AppRec sets receivers appRec institution typeId v to first organization ident typeId v`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().receiver.hcp.inst.typeId.v shouldBeEqualTo
            apprec.mottakerOrganisasjon.hovedIdent.typeId.verdi
    }

    @Test
    internal fun `OK AppRec sets receivers appRec id to first organization ident id`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().receiver.hcp.inst.hcPerson.first().id shouldBeEqualTo
            apprec.mottakerOrganisasjon.helsepersonell?.hovedIdent?.id
    }

    @Test
    internal fun `OK AppRec sets appRec status dn to OK`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().status.dn shouldBeEqualTo ApprecStatus.OK.dn
    }

    @Test
    internal fun `OK AppRec sets appRec status v to OK`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().status.v shouldBeEqualTo ApprecStatus.OK.v
    }

    @Test
    internal fun `OK AppRec sets appRec originalMsgId fn`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().originalMsgId.msgType.dn shouldBeEqualTo "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding"
    }

    @Test
    internal fun `OK AppRec sets appRec originalMsgId v`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().originalMsgId.msgType.v shouldBeEqualTo "SYKMELD"
    }

    @Test
    internal fun `OK AppRec sets appRec genDate as issueDate`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().originalMsgId.issueDate shouldBeEqualTo getDateTimeString(apprec.genDate)
    }

    @Test
    internal fun `OK AppRec sets appRec originalMsgId to msgid`() {
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        ff.get<XMLAppRec>().originalMsgId.id shouldBeEqualTo apprec.msgId
    }

    @Test
    internal fun `Error AppRec sets appRec error dn to duplicate`() {
        val apprecErrorinvalidFnrSize = createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()))
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)

        ff.get<XMLAppRec>().error.first().dn shouldBeEqualTo apprecErrorinvalidFnrSize.dn
    }

    @Test
    internal fun `Error AppRec sets appRec error v to duplicate`() {
        val apprecErrorinvalidFnrSize = createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()))
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)

        ff.get<XMLAppRec>().error.first().v shouldBeEqualTo apprecErrorinvalidFnrSize.v
    }

    @Test
    internal fun `Error AppRec sets appRec error s to duplicate`() {
        val apprecErrorinvalidFnrSize = createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff = marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf()))
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)

        ff.get<XMLAppRec>().error.first().s shouldBeEqualTo apprecErrorinvalidFnrSize.s
    }
}
