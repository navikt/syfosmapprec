package no.nav.syfo

import no.nav.syfo.apprec.ApprecStatus
import no.nav.syfo.model.ValidationResult

data class Apprec(
    val fellesformat: String,
    val apprecStatus: ApprecStatus,
    val tekstTilSykmelder: String?,
    val validationResult: ValidationResult?
)
