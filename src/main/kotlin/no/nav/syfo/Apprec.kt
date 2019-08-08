package no.nav.syfo

import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.syfo.apprec.ApprecStatus
import no.nav.syfo.model.ValidationResult

data class Apprec(
    val fellesformat: XMLEIFellesformat,
    val apprecStatus: ApprecStatus,
    val tekstTilSykmelder: String?,
    val validationResult: ValidationResult
)
