import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.LocalDateTime
import no.nav.syfo.Apprec
import no.nav.syfo.CS
import no.nav.syfo.Helsepersonell
import no.nav.syfo.Ident
import no.nav.syfo.Organisation
import no.nav.syfo.apprec.ApprecStatus

fun main() {

    val objectMapper: ObjectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    val apprec = Apprec(
            ediloggid = "1414243424522424mottak.1",
            msgId = "21313-1313-13--1313",
            msgTypeV = "SYKMELD",
            msgTypeDN = "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding",
            genDate = LocalDateTime.now(),
            apprecStatus = ApprecStatus.OK,
            tekstTilSykmelder = null,
            mottakerOrganisasjon = Organisation(
                    houvedIdent = Ident(id = "1234634567", typeId = CS(dn = "HER-id", v = "HER")),
                    navn = "Testlegesenteret",
                    tillegsIdenter = listOf(
                            Ident(id = "223456789",
                                    typeId = CS(dn = "Organisasjonsnummeret i Enhetsregister (Brønnøysund)", v = "ENH"))
                                ),
                    helsepersonell = Helsepersonell(
                            navn = "Per Hansen",
                            houvedIdent = Ident(id = "1234356", typeId = CS(dn = "HER-id", v = "HER")),
                            typeId = CS(dn = "HER-id", v = "HER"),
                            tillegsIdenter = listOf(
                                    Ident(id = "04030350265", typeId = CS(dn = "Fødselsnummer", v = "FNR")),
                                    Ident(id = "12343568", typeId = CS(dn = "HPR-nummer", v = "HPR"))
                                    )

                    )
            ),
            senderOrganisasjon = Organisation(
                    houvedIdent = Ident(id = "1234556", typeId = CS(dn = "HER-id", v = "HER")),
                    navn = "NAV IKT",
                    tillegsIdenter = listOf(
                            Ident(id = "1234556",
                                    typeId = CS(dn = "Organisasjonsnummeret i Enhetsregister (Brønnøysund)", v = "ENH"))
                    )

            ),
            validationResult = null

    )

    println(objectMapper.writeValueAsString(apprec))
}
