package no.nav.syfo.apprec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.StringReader
import java.time.LocalDateTime
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.syfo.get
import no.nav.syfo.serializeAppRec
import no.nav.syfo.util.getDateTimeString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ApprecMapperTest {
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

    private val apprecUnmarshaller: Unmarshaller =
        JAXBContext.newInstance(
                XMLEIFellesformat::class.java,
                XMLAppRec::class.java,
                XMLMottakenhetBlokk::class.java
            )
            .createUnmarshaller()

    private fun marshalAndUnmarshal(fellesformat: XMLEIFellesformat): XMLEIFellesformat =
        apprecUnmarshaller.unmarshal(StringReader(serializeAppRec(fellesformat)))
            as XMLEIFellesformat

    @Test
    internal fun `Duplicate AppRec has the same ediLoggId as the source`() {
        val apprecErrorDuplicate =
            createApprecError(
                "Duplikat! - Denne sykmeldingen er mottatt tidligere. Skal ikke sendes på nytt.",
            )
        val ff =
            marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec,
                    ApprecStatus.AVVIST,
                    listOf(),
                ),
            )
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        Assertions.assertEquals(apprec.ediloggid, ff.get<XMLMottakenhetBlokk>().ediLoggId)
    }

    @Test
    internal fun `Duplicate AppRec sets appRec status dn to Avvist`() {
        val apprecErrorDuplicate =
            createApprecError(
                "Duplikat! - Denne sykmeldingen er mottatt tidligere. Skal ikke sendes på nytt.",
            )
        val ff =
            marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec,
                    ApprecStatus.AVVIST,
                    listOf(),
                ),
            )
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        Assertions.assertEquals(ApprecStatus.AVVIST.dn, ff.get<XMLAppRec>().status.dn)
    }

    @Test
    internal fun `Duplicate AppRec sets appRec error v to duplicate`() {
        val apprecErrorDuplicate =
            createApprecError(
                "Duplikat! - Denne sykmeldingen er mottatt tidligere. Skal ikke sendes på nytt.",
            )
        val ff =
            marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec,
                    ApprecStatus.AVVIST,
                    listOf(),
                ),
            )
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        Assertions.assertEquals(apprecErrorDuplicate.v, ff.get<XMLAppRec>().error.first().v)
    }

    @Test
    internal fun `Duplicate AppRec sets appRec error s to duplicate`() {
        val apprecErrorDuplicate =
            createApprecError(
                "Duplikat! - Denne sykmeldingen er mottatt tidligere. Skal ikke sendes på nytt.",
            )
        val ff =
            marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec,
                    ApprecStatus.AVVIST,
                    listOf(),
                ),
            )
        ff.get<XMLAppRec>().error.add(apprecErrorDuplicate)
        Assertions.assertEquals(apprecErrorDuplicate.s, ff.get<XMLAppRec>().error.first().s)
    }

    @Test
    internal fun `OK AppRec should get correct genDate`() {
        val apprecWithMsgGenDate =
            marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec.copy(
                        genDate = LocalDateTime.parse("2021-03-03T12:02:02"),
                        msgGenDate = "2021-03-03T12:01:01+01:00",
                    ),
                    ApprecStatus.OK,
                    listOf(),
                ),
            )
        Assertions.assertEquals(
            "2021-03-03T12:01:01+01:00",
            apprecWithMsgGenDate.get<XMLAppRec>().originalMsgId.issueDate
        )
    }

    @Test
    internal fun `OK AppRec should get correct genDate with msgGenDate = null`() {
        val apprecWithoutMsgGenDate =
            marshalAndUnmarshal(
                createApprec(
                    apprec.ediloggid,
                    apprec.copy(genDate = LocalDateTime.parse("2021-03-03T12:02:02")),
                    ApprecStatus.OK,
                    listOf(),
                ),
            )
        Assertions.assertEquals(
            "2021-03-03T12:02:02",
            apprecWithoutMsgGenDate.get<XMLAppRec>().originalMsgId.issueDate
        )
    }

    @Test
    internal fun `OK AppRec sets ebRole to ebRoleNav`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            ApprecConstant.EBROLENAV.string,
            ff.get<XMLMottakenhetBlokk>().ebRole
        )
    }

    @Test
    internal fun `OK AppRec sets ebService`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals("Sykmelding", ff.get<XMLMottakenhetBlokk>().ebService)
    }

    @Test
    internal fun `OK AppRec sets ebAction`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            ApprecConstant.EBACTIONSVARMELDING.string,
            ff.get<XMLMottakenhetBlokk>().ebAction
        )
    }

    @Test
    internal fun `OK AppRec sets appRec message type`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(ApprecConstant.APPREC.string, ff.get<XMLAppRec>().msgType.v)
    }

    @Test
    internal fun `OK AppRec sets appRec miGversion`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            ApprecConstant.APPRECVERSIONV1_0.string,
            ff.get<XMLAppRec>().miGversion
        )
    }

    @Test
    internal fun `OK AppRec setsappRec id to ediLoggId`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(apprec.ediloggid, ff.get<XMLAppRec>().id)
    }

    @Test
    internal fun `OK AppRec senders appRec sender institution name to receiver organizationName`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.senderOrganisasjon.navn,
            ff.get<XMLAppRec>().sender.hcp.inst.name
        )
    }

    @Test
    internal fun `OK AppRec senders appRec institution id to first organization ident id`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.senderOrganisasjon.hovedIdent.id,
            ff.get<XMLAppRec>().sender.hcp.inst.id
        )
    }

    @Test
    internal fun `OK AppRec senders appRec institution typeId dn to first organization ident typeId dn`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.senderOrganisasjon.hovedIdent.typeId.beskrivelse,
            ff.get<XMLAppRec>().sender.hcp.inst.typeId.dn
        )
    }

    @Test
    internal fun `OK AppRec senders appRec institution typeId v to first organization ident typeId v`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.senderOrganisasjon.hovedIdent.typeId.verdi,
            ff.get<XMLAppRec>().sender.hcp.inst.typeId.v
        )
    }

    @Test
    internal fun `OK AppRec senders first additional appRec institution id to second organization ident id`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.senderOrganisasjon.tilleggsIdenter?.first()?.id,
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().id,
        )
    }

    @Test
    internal fun `OK AppRec senders senders first additional appRec institution typeId dn to second organization ident typeId dn`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.beskrivelse,
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.dn,
        )
    }

    @Test
    internal fun `OK AppRec first additional appRec institution typeId v to second organization ident typeId v`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.senderOrganisasjon.tilleggsIdenter?.first()?.typeId?.verdi,
            ff.get<XMLAppRec>().sender.hcp.inst.additionalId.first().type.v,
        )
    }

    @Test
    internal fun `OK AppRec receivers appRec institution name to sender organizationName`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.mottakerOrganisasjon.navn,
            ff.get<XMLAppRec>().receiver.hcp.inst.name,
        )
    }

    @Test
    internal fun `OK AppRec sets receivers appRec institution id to first sender organization ident id`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.mottakerOrganisasjon.hovedIdent.id,
            ff.get<XMLAppRec>().receiver.hcp.inst.id,
        )
    }

    @Test
    internal fun `OK AppRec sets receivers appRec institution typeId dn to first sender organization ident typeId dn`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.mottakerOrganisasjon.hovedIdent.typeId.beskrivelse,
            ff.get<XMLAppRec>().receiver.hcp.inst.typeId.dn,
        )
    }

    @Test
    internal fun `OK AppRec sets receivers appRec institution typeId v to first organization ident typeId v`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.mottakerOrganisasjon.hovedIdent.typeId.verdi,
            ff.get<XMLAppRec>().receiver.hcp.inst.typeId.v,
        )
    }

    @Test
    internal fun `OK AppRec sets receivers appRec id to first organization ident id`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.mottakerOrganisasjon.helsepersonell?.hovedIdent?.id,
            ff.get<XMLAppRec>().receiver.hcp.inst.hcPerson.first().id,
        )
    }

    @Test
    internal fun `OK AppRec sets appRec status dn to OK`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            ApprecStatus.OK.dn,
            ff.get<XMLAppRec>().status.dn,
        )
    }

    @Test
    internal fun `OK AppRec sets appRec status v to OK`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            ApprecStatus.OK.v,
            ff.get<XMLAppRec>().status.v,
        )
    }

    @Test
    internal fun `OK AppRec sets appRec originalMsgId dn`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding",
            ff.get<XMLAppRec>().originalMsgId.msgType.dn,
        )
    }

    @Test
    internal fun `OK AppRec sets appRec originalMsgId v`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            "SYKMELD",
            ff.get<XMLAppRec>().originalMsgId.msgType.v,
        )
    }

    @Test
    internal fun `OK AppRec sets appRec genDate as issueDate`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            getDateTimeString(apprec.genDate),
            ff.get<XMLAppRec>().originalMsgId.issueDate,
        )
    }

    @Test
    internal fun `OK AppRec sets appRec originalMsgId to msgid`() {
        val ff =
            marshalAndUnmarshal(createApprec(apprec.ediloggid, apprec, ApprecStatus.OK, listOf()))
        Assertions.assertEquals(
            apprec.msgId,
            ff.get<XMLAppRec>().originalMsgId.id,
        )
    }

    @Test
    internal fun `Error AppRec sets appRec error dn to duplicate`() {
        val apprecErrorinvalidFnrSize =
            createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff =
            marshalAndUnmarshal(
                createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf())
            )
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)

        Assertions.assertEquals(
            apprecErrorinvalidFnrSize.dn,
            ff.get<XMLAppRec>().error.first().dn,
        )
    }

    @Test
    internal fun `Error AppRec sets appRec error v to duplicate`() {
        val apprecErrorinvalidFnrSize =
            createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff =
            marshalAndUnmarshal(
                createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf())
            )
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)

        Assertions.assertEquals(
            apprecErrorinvalidFnrSize.v,
            ff.get<XMLAppRec>().error.first().v,
        )
    }

    @Test
    internal fun `Error AppRec sets appRec error s to duplicate`() {
        val apprecErrorinvalidFnrSize =
            createApprecError("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
        val ff =
            marshalAndUnmarshal(
                createApprec(apprec.ediloggid, apprec, ApprecStatus.AVVIST, listOf())
            )
        ff.get<XMLAppRec>().error.add(apprecErrorinvalidFnrSize)

        Assertions.assertEquals(
            apprecErrorinvalidFnrSize.s,
            ff.get<XMLAppRec>().error.first().s,
        )
    }
}
