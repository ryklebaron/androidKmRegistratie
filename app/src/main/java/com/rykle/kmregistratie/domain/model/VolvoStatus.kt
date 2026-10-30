package com.rykle.kmregistratie.domain.model

sealed interface OdometerResult {
    data class Success(val km: Int) : OdometerResult
    /** [reason] is een Nederlandse, voor de gebruiker leesbare uitleg. */
    data class Failure(val reason: String) : OdometerResult
}

/** Uitkomst van de laatste aanroep naar de Volvo API, voor de statusbalk. */
sealed interface VolvoStatus {
    data object Unknown : VolvoStatus
    data object Checking : VolvoStatus
    data class Connected(val km: Int, val atMillis: Long) : VolvoStatus
    data class Failed(val reason: String, val atMillis: Long) : VolvoStatus
}
