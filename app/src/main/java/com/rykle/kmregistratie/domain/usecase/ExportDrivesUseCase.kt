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
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale("nl", "NL"))
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("nl", "NL"))

    suspend operator fun invoke(from: Long? = null, to: Long? = null): String {
        val drives = driveRepository.getAllDrives().first()
            .filter { !it.isActive }
            .let { list ->
                if (from != null && to != null) list.filter { it.startTime in from..to } else list
            }
        return buildString {
            appendLine("Datum,Starttijd,Eindtijd,Type,Start Lat,Start Lng,Eind Lat,Eind Lng,Start KM,Eind KM,Km Gereden,Opmerking")
            drives.forEach { drive -> appendLine(drive.toCsvRow()) }
        }
    }

    private fun Drive.toCsvRow(): String {
        val date = dateFormat.format(Date(startTime))
        val start = timeFormat.format(Date(startTime))
        val end = endTime?.let { timeFormat.format(Date(it)) } ?: ""
        val type = if (driveType == DriveType.BUSINESS) "Zakelijk" else "Privé"
        val remark = if (hasKmMismatch) "KM MISMATCH" else ""
        return "$date,$start,$end,$type,$startLat,$startLng,${endLat ?: ""},${endLng ?: ""},${startKm ?: ""},${endKm ?: ""},${kmDriven ?: ""},$remark"
    }
}
