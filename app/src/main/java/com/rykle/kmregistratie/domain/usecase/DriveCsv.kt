package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV-formaat van de rittenlijst: voor de boekhouding én om terug te zetten (Nextcloud, bestand).
 * Inlezen gaat op kolomnaam, zodat ook oudere exports (lat/lng-kolommen, of "Startlocatie")
 * werken.
 */
object DriveCsv {

    /** Een ingelezen rit; het project staat er als naam in en wordt pas bij import een id. */
    data class Row(val drive: Drive, val projectName: String?)

    private const val MISMATCH = "KM MISMATCH"
    private const val PENDING = "EINDSTAND ONBEKEND"

    private val header = listOf(
        "Datum", "Starttijd", "Einddatum", "Eindtijd", "Type", "Project", "Notitie",
        "Startadres", "Eindadres", "Start KM", "Eind KM", "Zakelijk km", "Privé km", "Duur (min)",
        "Opmerking", "Start Lat", "Start Lng", "Eind Lat", "Eind Lng",
    )
    private val dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun write(drives: List<Drive>, projectNames: Map<Long, String>, zone: ZoneId = ZoneId.systemDefault()): String =
        buildString {
            appendLine(header.joinToString(","))
            drives.forEach { appendLine(row(it, projectNames, zone).joinToString(",") { v -> quote(v) }) }
        }

    private fun row(d: Drive, projectNames: Map<Long, String>, zone: ZoneId): List<String> {
        val start = Instant.ofEpochMilli(d.startTime).atZone(zone)
        val end = d.endTime?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val km = d.kmDriven?.toString().orEmpty()
        return listOf(
            start.format(dateFmt),
            start.format(timeFmt),
            end?.format(dateFmt).orEmpty(),
            end?.format(timeFmt).orEmpty(),
            if (d.driveType == DriveType.BUSINESS) "Zakelijk" else "Privé",
            d.projectId?.let { projectNames[it] }.orEmpty(),
            d.note.orEmpty().replace('\n', ' '),
            d.startAddress.orEmpty(),
            d.endAddress.orEmpty(),
            d.startKm?.toString().orEmpty(),
            d.endKm?.toString().orEmpty(),
            if (d.driveType == DriveType.BUSINESS) km else "",
            if (d.driveType == DriveType.PRIVATE) km else "",
            d.endTime?.let { ((it - d.startTime) / 60_000).toString() }.orEmpty(),
            listOfNotNull(MISMATCH.takeIf { d.hasKmMismatch }, PENDING.takeIf { d.kmPending }).joinToString(" / "),
            coord(d.startLat), coord(d.startLng), coord(d.endLat), coord(d.endLng),
        )
    }

    private fun coord(v: Double?) = if (v == null || v == 0.0) "" else v.toString()

    private fun quote(v: String) =
        if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    /** Leest alle bruikbare ritten; regels zonder geldige startdatum worden overgeslagen. */
    fun read(csv: String, zone: ZoneId = ZoneId.systemDefault()): List<Row> {
        val records = parse(csv.removePrefix("﻿")).filter { r -> r.any { it.isNotBlank() } }
        if (records.isEmpty()) return emptyList()
        val columns = records.first().mapIndexed { i, name -> name.trim().lowercase() to i }.toMap()
        fun List<String>.col(vararg names: String): String =
            names.firstNotNullOfOrNull { columns[it] }?.let { getOrNull(it) }?.trim().orEmpty()

        return records.drop(1).mapNotNull { r ->
            val startDate = parseDate(r.col("datum")) ?: return@mapNotNull null
            val startTime = startDate.atTime(parseTime(r.col("starttijd")) ?: LocalTime.MIDNIGHT).atZone(zone)
            val endTime = parseTime(r.col("eindtijd"))?.let { t ->
                val endDate = parseDate(r.col("einddatum")) ?: startDate
                var end = endDate.atTime(t).atZone(zone)
                // Oud formaat zonder einddatum: rit over middernacht.
                if (end.isBefore(startTime)) end = end.plusDays(1)
                end
            }
            val remark = r.col("opmerking")
            val startLoc = location(r.col("startadres", "startlocatie"), r.col("start lat"), r.col("start lng"))
            val endLoc = location(r.col("eindadres", "eindlocatie"), r.col("eind lat"), r.col("eind lng"))
            val drive = Drive(
                startTime = startTime.toInstant().toEpochMilli(),
                endTime = endTime?.toInstant()?.toEpochMilli(),
                startLat = startLoc.lat ?: 0.0,
                startLng = startLoc.lng ?: 0.0,
                startAddress = startLoc.address,
                endLat = if (endTime != null) endLoc.lat ?: 0.0 else null,
                endLng = if (endTime != null) endLoc.lng ?: 0.0 else null,
                endAddress = endLoc.address,
                driveType = if (r.col("type").equals("zakelijk", ignoreCase = true)) DriveType.BUSINESS else DriveType.PRIVATE,
                startKm = r.col("start km").toIntOrNull(),
                endKm = r.col("eind km").toIntOrNull(),
                hasKmMismatch = MISMATCH in remark,
                kmPending = PENDING in remark,
                note = r.col("notitie").ifBlank { null },
            )
            Row(drive, r.col("project").ifBlank { null })
        }
    }

    private class Location(val address: String?, val lat: Double?, val lng: Double?)

    private val coordPattern = Regex("""^(-?\d+\.\d+)[ ,]+(-?\d+\.\d+)$""")

    /** Oude exports zetten coördinaten ("52.12345 6.12345") in de locatiekolom als er geen adres was. */
    private fun location(text: String, lat: String, lng: String): Location {
        coordPattern.matchEntire(text)?.let { m ->
            return Location(null, m.groupValues[1].toDouble(), m.groupValues[2].toDouble())
        }
        return Location(text.ifBlank { null }, lat.toDoubleOrNull(), lng.toDoubleOrNull())
    }

    private fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s, dateFmt) }.getOrNull()

    private fun parseTime(s: String): LocalTime? =
        if (s.isBlank()) null else runCatching { LocalTime.parse(if (s.length == 5) "$s:00" else s, timeFmt) }.getOrNull()

    /** RFC 4180: velden tussen aanhalingstekens mogen komma's, "" en regeleinden bevatten. */
    internal fun parse(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> { fields.add(field.toString()); field.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    fields.add(field.toString()); field.clear()
                    records.add(fields); fields = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            records.add(fields)
        }
        return records
    }
}
