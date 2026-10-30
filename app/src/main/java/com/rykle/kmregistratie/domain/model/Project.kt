package com.rykle.kmregistratie.domain.model

/**
 * Klus of opdracht waar ritten aan gekoppeld worden, voor inzicht in km en uren per project.
 * Koppelen is alleen een label op de rit: kilometerstanden worden nooit aangepast.
 * Met een locatie kan de app ritten van en naar die plek voorstellen.
 */
data class Project(
    val id: Long = 0,
    val name: String,
    val archived: Boolean = false,
    val client: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null,
) {
    val hasLocation: Boolean get() = (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) || !address.isNullOrBlank()
}
