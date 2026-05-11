package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.repository.DriveRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportDrivesUseCase @Inject constructor(
    private val driveRepository: DriveRepository
) {
    suspend operator fun invoke(from: Long? = null, to: Long? = null, ids: Set<Long>? = null): String {
        val drives = driveRepository.getAllDrives().first()
            .filter { !it.isActive }
            .let { list ->
                when {
                    ids != null -> list.filter { it.id in ids }
                    from != null && to != null -> list.filter { it.startTime in from..to }
                    else -> list
                }
            }
        val dateFmt = SimpleDateFormat("dd-MM-yyyy", Locale("nl", "NL"))
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale("nl", "NL"))
        return buildString {
            appendLine("Datum,Starttijd,Eindtijd,Type,Startlocatie,Eindlocatie,Start KM,Eind KM,Zakelijk km,Privé km,Opmerking")
            drives.forEach { drive -> appendLine(drive.toCsvRow(dateFmt, timeFmt)) }
        }
    }

    private fun Drive.toCsvRow(dateFmt: SimpleDateFormat, timeFmt: SimpleDateFormat): String {
        val date = dateFmt.format(Date(startTime))
        val start = timeFmt.format(Date(startTime))
        val end = endTime?.let { timeFmt.format(Date(it)) } ?: ""
        val type = if (driveType == DriveType.BUSINESS) "Zakelijk" else "Privé"
        val startLoc = csvQuote(startAddress ?: formatCoords(startLat, startLng) ?: "")
        val endLoc = csvQuote(endAddress ?: formatCoords(endLat, endLng) ?: "")
        val zakelijkKm = if (driveType == DriveType.BUSINESS) kmDriven?.toString() ?: "" else ""
        val privetKm = if (driveType == DriveType.PRIVATE) kmDriven?.toString() ?: "" else ""
        val remark = if (hasKmMismatch) "KM MISMATCH" else ""
        return "$date,$start,$end,$type,$startLoc,$endLoc,${startKm ?: ""},${endKm ?: ""},$zakelijkKm,$privetKm,$remark"
    }

    private fun formatCoords(lat: Double?, lng: Double?): String? {
        if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) return null
        return "%.5f %.5f".format(lat, lng)
    }

    private fun csvQuote(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n'))
            "\"${value.replace("\"", "\"\"")}\""
        else value
}
