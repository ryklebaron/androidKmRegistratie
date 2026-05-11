package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.repository.DriveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

class ImportDrivesUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
) {
    data class ImportResult(val imported: Int, val skipped: Int)

    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
    private val dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    suspend operator fun invoke(csvContent: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val lines = csvContent.lines().filter { it.isNotBlank() }
            if (lines.size < 2) return@runCatching ImportResult(0, 0)

            var imported = 0
            var skipped = 0

            for (line in lines.drop(1)) {
                val f = parseCsvLine(line)
                if (f.size < 8) continue

                val startTime = parseDateTime(f[0], f[1]) ?: continue

                if (driveRepository.getDriveByStartTime(startTime) != null) {
                    skipped++
                    continue
                }

                val endTime = f[2].trim().takeIf { it.isNotBlank() }?.let { parseDateTime(f[0], it) }
                val type = if (f[3].trim() == "Zakelijk") DriveType.BUSINESS else DriveType.PRIVATE
                val startAddress = f[4].trim().takeIf { it.isNotBlank() }
                val endAddress = f[5].trim().takeIf { it.isNotBlank() }
                val startKm = f[6].trim().toIntOrNull()
                val endKm = f[7].trim().toIntOrNull()
                val hasKmMismatch = f.getOrElse(10) { "" }.trim() == "KM MISMATCH"

                driveRepository.insertFullDrive(
                    startTime = startTime, endTime = endTime,
                    startLat = 0.0, startLng = 0.0, startAddress = startAddress,
                    endLat = if (endTime != null) 0.0 else null,
                    endLng = if (endTime != null) 0.0 else null,
                    endAddress = endAddress,
                    driveType = type, startKm = startKm, endKm = endKm,
                    hasKmMismatch = hasKmMismatch,
                )
                imported++
            }

            ImportResult(imported, skipped)
        }
    }

    private fun parseDateTime(date: String, time: String): Long? {
        val dateStr = date.trim()
        val timeStr = time.trim()
        if (timeStr.isNotBlank()) {
            try {
                return LocalDateTime.parse("$dateStr $timeStr", dateTimeFmt)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) { }
        }
        return try {
            LocalDate.parse(dateStr, dateFmt)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) { null }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
}
