import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.Apprec
import no.nav.syfo.Helsepersonell
import no.nav.syfo.Ident
import no.nav.syfo.Kodeverdier
import no.nav.syfo.Organisation
import no.nav.syfo.apprec.ApprecStatus
import java.time.LocalDateTime

fun main() {

    val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    val apprec = Apprec(
        ediloggid = "1414243424522424mottak.1",
        msgId = "21313-1313-13--1313",
        msgTypeVerdi = "SYKMELD",
        msgTypeBeskrivelse = "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding",
        genDate = LocalDateTime.now(),
        apprecStatus = ApprecStatus.OK,
        msgGenDate = "2021-03-03T12:01:01",
        tekstTilSykmelder = null,
        mottakerOrganisasjon = Organisation(
            hovedIdent = Ident(id = "1234634567", typeId = Kodeverdier(beskrivelse = "HER-id", verdi = "HER")),
            navn = "Testlegesenteret",
            tilleggsIdenter = listOf(
                Ident(
                    id = "223456789",
                    typeId = Kodeverdier(beskrivelse = "Organisasjonsnummeret i Enhetsregister (Brønnøysund)", verdi = "ENH")
                )
            ),
            helsepersonell = Helsepersonell(
                navn = "Per Hansen",
                hovedIdent = Ident(id = "1234356", typeId = Kodeverdier(beskrivelse = "HER-id", verdi = "HER")),
                typeId = Kodeverdier(beskrivelse = "HER-id", verdi = "HER"),
                tilleggsIdenter = listOf(
                    Ident(id = "04030350265", typeId = Kodeverdier(beskrivelse = "Fødselsnummer", verdi = "FNR")),
                    Ident(id = "12343568", typeId = Kodeverdier(beskrivelse = "HPR-nummer", verdi = "HPR"))
                )

            )
        ),
        senderOrganisasjon = Organisation(
            hovedIdent = Ident(id = "1234556", typeId = Kodeverdier(beskrivelse = "HER-id", verdi = "HER")),
            navn = "NAV IKT",
            tilleggsIdenter = listOf(
                Ident(
                    id = "1234556",
                    typeId = Kodeverdier(beskrivelse = "Organisasjonsnummeret i Enhetsregister (Brønnøysund)", verdi = "ENH")
                )
            )

        ),
        validationResult = null

    )

    println(objectMapper.writeValueAsString(apprec))
}
