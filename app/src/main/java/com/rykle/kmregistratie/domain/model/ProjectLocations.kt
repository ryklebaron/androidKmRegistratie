package com.rykle.kmregistratie.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Herkent ritten die (nagenoeg) bij een projectlocatie beginnen of eindigen: op GPS-afstand,
 * of op adres (straat + huisnummer + plaats) als een van beide geen GPS heeft.
 */
object ProjectLocations {

    const val RADIUS_METERS = 150.0

    /** Thuis krijgt GPS-ruis buuradressen tot ± 60 m ver; alles binnen deze straal is "thuis". */
    const val HOME_RADIUS_METERS = 75.0

    /** Rit die bij de projectlocatie aankomt ([arrives]) en/of daar vertrekt ([departs]). */
    data class Match(val drive: Drive, val arrives: Boolean, val departs: Boolean)

    /** Ritpunten die thuis liggen ([home]) tellen nooit mee: vanaf huis vertrekken is geen project. */
    fun nearbyDrives(project: Project, drives: List<Drive>, home: SavedLocation? = null, radius: Double = RADIUS_METERS): List<Match> {
        if (!project.hasLocation) return emptyList()
        return drives.filter { !it.isActive }.mapNotNull { d ->
            val arrives = !isHome(home, d.endLat, d.endLng, d.endAddress) &&
                isNear(project, d.endLat, d.endLng, d.endAddress, radius)
            val departs = !isHome(home, d.startLat, d.startLng, d.startAddress) &&
                isNear(project, d.startLat, d.startLng, d.startAddress, radius)
            if (arrives || departs) Match(d, arrives, departs) else null
        }.sortedByDescending { it.drive.startTime }
    }

    /**
     * Actief project bij aankomst (of anders vertrek) van een rit zonder project.
     * [rejected] = project-ids waarvoor de gebruiker bij deze rit al "nee" zei.
     */
    fun suggestFor(
        drive: Drive,
        projects: List<Project>,
        rejected: Set<Long> = emptySet(),
        home: SavedLocation? = null,
        radius: Double = RADIUS_METERS,
    ): Project? {
        if (drive.projectId != null || drive.isActive) return null
        val candidates = projects.filter { !it.archived && it.hasLocation && it.id !in rejected }
        val endAtHome = isHome(home, drive.endLat, drive.endLng, drive.endAddress)
        val startAtHome = isHome(home, drive.startLat, drive.startLng, drive.startAddress)
        return candidates.firstOrNull { !endAtHome && isNear(it, drive.endLat, drive.endLng, drive.endAddress, radius) }
            ?: candidates.firstOrNull { !startAtHome && isNear(it, drive.startLat, drive.startLng, drive.startAddress, radius) }
    }

    /** Thuis: binnen [HOME_RADIUS_METERS] van het thuispunt, of (zonder GPS) hetzelfde adres. */
    fun isHome(home: SavedLocation?, lat: Double?, lng: Double?, address: String?): Boolean {
        if (home == null) return false
        if (hasGps(lat, lng) && hasGps(home.latitude, home.longitude)) {
            return distanceMeters(home.latitude, home.longitude, lat!!, lng!!) <= HOME_RADIUS_METERS
        }
        val key = addressKey(address)
        return key != null && key == addressKey(home.address)
    }

    /**
     * Waarschijnlijk thuisadres: de plek waar de meeste ritten beginnen of eindigen
     * (meest voorkomend adres, met de mediaan van de GPS-punten bij dat adres).
     */
    fun guessHome(drives: List<Drive>): Pair<SavedLocation, Int>? {
        data class Point(val address: String, val lat: Double, val lng: Double)
        val points = drives.flatMap { d ->
            listOfNotNull(
                d.startAddress?.let { Point(it, d.startLat, d.startLng) },
                d.endAddress?.let { a -> Point(a, d.endLat ?: 0.0, d.endLng ?: 0.0) },
            )
        }
        val (key, group) = points.groupBy { addressKey(it.address) }
            .filterKeys { it != null }
            .maxByOrNull { it.value.size } ?: return null
        val gps = group.filter { hasGps(it.lat, it.lng) }
        if (gps.isEmpty() || key == null) return null
        val address = group.groupingBy { it.address }.eachCount().maxBy { it.value }.key
        val home = SavedLocation(
            address = address,
            latitude = gps.map { it.lat }.sorted()[gps.size / 2],
            longitude = gps.map { it.lng }.sorted()[gps.size / 2],
        )
        return home to group.size
    }

    private fun isNear(p: Project, lat: Double?, lng: Double?, address: String?, radius: Double): Boolean {
        if (hasGps(p.lat, p.lng) && hasGps(lat, lng)) {
            return distanceMeters(p.lat!!, p.lng!!, lat!!, lng!!) <= radius
        }
        val a = addressKey(p.address)
        return a != null && a == addressKey(address)
    }

    private fun hasGps(lat: Double?, lng: Double?) = lat != null && lng != null && (lat != 0.0 || lng != 0.0)

    /** "Korenlaan 18, Middelstum, Groningen" en "korenlaan 18,  Middelstum" → "korenlaan 18|middelstum". */
    internal fun addressKey(address: String?): String? {
        val parts = address?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }.orEmpty()
        return if (parts.size >= 2) "${parts[0]}|${parts[1]}" else parts.firstOrNull()
    }

    /** Haversine-afstand in meters. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
}
