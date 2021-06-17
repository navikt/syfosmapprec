package no.nav.syfo.apprec

import no.nav.helse.apprecV1.XMLAdditionalId
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.apprecV1.XMLCS
import no.nav.helse.apprecV1.XMLHCP
import no.nav.helse.apprecV1.XMLHCPerson
import no.nav.helse.apprecV1.XMLInst
import no.nav.helse.apprecV1.XMLOriginalMsgId
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.syfo.Apprec
import no.nav.syfo.Helsepersonell
import no.nav.syfo.Ident
import no.nav.syfo.Kodeverdier
import no.nav.syfo.Organisation
import no.nav.syfo.SyfoSmApprecConstant
import no.nav.syfo.apprecJaxbMarshaller
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.util.getDateTimeString
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.xml.parsers.DocumentBuilderFactory
import no.nav.helse.apprecV1.XMLCV as AppRecCV

private val log = LoggerFactory.getLogger("no.nav.syfo.apprec.ApprecMapper")

fun apprecToElement(apprec: XMLAppRec): Element {
    val document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .newDocument()
    apprecJaxbMarshaller.marshal(apprec, document)
    return document.documentElement
}

fun createApprec(
    ediloggid: String,
    apprec: Apprec,
    apprecStatus: ApprecStatus,
    apprecErrors: List<AppRecCV>
): XMLEIFellesformat {
    val msgInfotypeVerdi = apprec.msgTypeVerdi
    val msgInfotypeBeskrivelse = apprec.msgTypeBeskrivelse
    val msgInfoGenDate: String = apprec.msgGenDate ?: getDateTimeString(apprec.genDate)
    when (apprec.msgGenDate) {
        null -> log.info("Using old datetime: $msgInfoGenDate")
        else -> log.info("Using original datetime: $msgInfoGenDate")
    }
    val msgId = apprec.msgId
    val senderOrganisation = apprec.senderOrganisasjon
    val mottakerOrganisation = apprec.mottakerOrganisasjon
    val fellesformatApprec = XMLEIFellesformat().apply {
        any.add(
            XMLMottakenhetBlokk().apply {
                ediLoggId = ediloggid
                ebRole = SyfoSmApprecConstant.EBROLENAV.string
                ebService = SyfoSmApprecConstant.EBSERVICESYKMELDING.string
                ebAction = SyfoSmApprecConstant.EBACTIONSVARMELDING.string
            }
        )

        any.add(
            apprecToElement(
                XMLAppRec().apply {
                    msgType = XMLCS().apply {
                        v = SyfoSmApprecConstant.APPREC.string
                    }
                    miGversion = SyfoSmApprecConstant.APPRECVERSIONV1_0.string
                    genDate = getDateTimeString(OffsetDateTime.now(ZoneOffset.UTC))
                    id = ediloggid

                    sender = XMLAppRec.Sender().apply {
                        hcp = senderOrganisation.intoHCP()
                    }

                    receiver = XMLAppRec.Receiver().apply {
                        hcp = mottakerOrganisation.intoHCP()
                    }

                    status = XMLCS().apply {
                        v = apprecStatus.v
                        dn = apprecStatus.dn
                    }

                    originalMsgId = XMLOriginalMsgId().apply {
                        msgType = XMLCS().apply {
                            v = msgInfotypeVerdi
                            dn = msgInfotypeBeskrivelse
                        }
                        issueDate = msgInfoGenDate
                        id = msgId
                    }

                    error.addAll(apprecErrors)
                }
            )
        )
    }

    return fellesformatApprec
}

fun Helsepersonell.intoHCPerson(): XMLHCPerson = XMLHCPerson().apply {
    name = navn
    id = hovedIdent.id
    typeId = hovedIdent.typeId.intoXMLCS()
    if (!tilleggsIdenter.isNullOrEmpty()) {
        additionalId += tilleggsIdenter
    }
}

fun Organisation.intoHCP(): XMLHCP = XMLHCP().apply {
    inst = hovedIdent.intoInst().apply {
        name = navn
        if (!tilleggsIdenter.isNullOrEmpty()) {
            additionalId += tilleggsIdenter
        }

        if (helsepersonell != null) {
            hcPerson += helsepersonell.intoHCPerson()
        }
    }
}

fun Ident.intoInst(): XMLInst {
    val ident = this
    return XMLInst().apply {
        id = ident.id
        typeId = ident.typeId.intoXMLCS()
    }
}

fun Kodeverdier.intoXMLCS(): XMLCS {
    val cs = this
    return XMLCS().apply {
        dn = cs.beskrivelse
        v = cs.verdi
    }
}

operator fun MutableList<XMLAdditionalId>.plusAssign(identer: Iterable<Ident>) {
    this.addAll(identer.map { it.intoAdditionalId() })
}

fun Ident.intoAdditionalId(): XMLAdditionalId {
    val ident = this
    return XMLAdditionalId().apply {
        id = ident.id
        type = XMLCS().apply {
            dn = ident.typeId.beskrivelse
            v = ident.typeId.verdi
        }
    }
}

fun RuleInfo.toApprecCV(): AppRecCV {
    val ruleInfo = this
    return createApprecError(ruleInfo.messageForSender)
}

fun createApprecError(textToTreater: String?): AppRecCV = AppRecCV().apply {
    dn = textToTreater ?: ""
    v = "2.16.578.1.12.4.1.1.8221"
    s = "X99"
}
