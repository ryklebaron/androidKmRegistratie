package com.rykle.kmregistratie.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rykle.kmregistratie.ui.viewmodel.SyncStatus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rykle.kmregistratie.R
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.ui.theme.MismatchRed
import com.rykle.kmregistratie.ui.viewmodel.DriveViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ── Period ────────────────────────────────────────────────────────────────────

private enum class Period(val label: String) {
    DAY("Dag"), WEEK("Week"), MONTH("Maand"), ALL("Alles"), CUSTOM("Aangepast…")
}

private fun periodToRange(period: Period, customFrom: Long?, customTo: Long?): Pair<Long, Long>? {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return when (period) {
        Period.DAY -> {
            val from = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            from to cal.timeInMillis
        }
        Period.WEEK -> {
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val from = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            from to cal.timeInMillis
        }
        Period.MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val from = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            from to cal.timeInMillis
        }
        Period.ALL -> null
        Period.CUSTOM -> if (customFrom != null && customTo != null) customFrom to customTo else null
    }
}

private fun utcMidnightToLocalStart(utcMs: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
    return Calendar.getInstance().apply {
        set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun utcMidnightToLocalEnd(utcMs: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
    return Calendar.getInstance().apply {
        set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DriveListScreen(viewModel: DriveViewModel, onDriveClick: (Long) -> Unit = {}) {
    val drives by viewModel.drives.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val context = LocalContext.current

    var selectedPeriod by rememberSaveable { mutableStateOf(Period.MONTH) }
    var customFrom by rememberSaveable { mutableStateOf<Long?>(null) }
    var customTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val selectionMode = selectedIds.isNotEmpty()

    val range = remember(selectedPeriod, customFrom, customTo) {
        periodToRange(selectedPeriod, customFrom, customTo)
    }
    val completedDrives = remember(drives) { drives.filter { !it.isActive } }
    val filteredDrives = remember(completedDrives, range) {
        if (range == null) completedDrives
        else completedDrives.filter { it.startTime in range.first..range.second }
    }
    val businessKm = remember(filteredDrives) {
        filteredDrives.filter { it.driveType == DriveType.BUSINESS }.sumOf { it.kmDriven ?: 0 }
    }
    val privateKm = remember(filteredDrives) {
        filteredDrives.filter { it.driveType == DriveType.PRIVATE }.sumOf { it.kmDriven ?: 0 }
    }
    val activeDrive = drives.firstOrNull { it.isActive }

    BackHandler(enabled = selectionMode) { selectedIds = emptySet() }

    // ── Date range picker ─────────────────────────────────────────────────
    if (showDateRangePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        customFrom = utcMidnightToLocalStart(start)
                        customTo = utcMidnightToLocalEnd(end)
                        selectedPeriod = Period.CUSTOM
                    }
                    showDateRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Annuleren") }
            }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
        }
    }

    // ── Start dialog ──────────────────────────────────────────────────────
    if (showStartDialog) {
        val canContinue = remember { viewModel.canContinueDrive() }
        StartDriveDialog(
            canContinue = canContinue,
            onBusiness = { viewModel.startDrive(DriveType.BUSINESS); showStartDialog = false },
            onPrivate = { viewModel.startDrive(DriveType.PRIVATE); showStartDialog = false },
            onContinue = { viewModel.continueDrive(); showStartDialog = false },
            onDismiss = { showStartDialog = false }
        )
    }

    // ── Export/import bottom sheet ────────────────────────────────────────
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importDrives(context, uri) { imported, skipped, error ->
                val msg = if (error) "Import mislukt"
                    else "$imported ritten hersteld, $skipped al aanwezig"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showExportSheet) {
        ExportSheet(
            drives = drives,
            nextcloudConfigured = viewModel.nextcloudConfigured,
            onDismiss = { showExportSheet = false },
            onExport = { from, to ->
                viewModel.exportAndShare(context, from, to)
                showExportSheet = false
            },
            onImportFromNextcloud = {
                showExportSheet = false
                viewModel.importFromNextcloud { imported, skipped, error ->
                    val msg = if (error) "Nextcloud herstel mislukt"
                        else "$imported ritten hersteld, $skipped al aanwezig"
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onImport = {
                showExportSheet = false
                importLauncher.launch("text/*")
            }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────
    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("${selectedIds.size} ritten verwijderen?") },
            text = { Text("Dit kan niet ongedaan worden gemaakt.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMultipleDrives(selectedIds)
                        selectedIds = emptySet()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Verwijderen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Annuleren") }
            }
        )
    }

    // ── Main content ──────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Header — normal or selection mode
        if (selectionMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${selectedIds.size} geselecteerd", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { selectedIds = emptySet() }) { Text("Annuleren") }
            }
        } else {
            Text(stringResource(R.string.screen_title_drives), style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Period.entries) { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = {
                        if (period == Period.CUSTOM) showDateRangePicker = true
                        else selectedPeriod = period
                    },
                    label = { Text(period.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SummaryCard(businessKm = businessKm, privateKm = privateKm, count = filteredDrives.size)

        Spacer(Modifier.height(12.dp))

        // Buttons — normal or selection mode
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selectionMode) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportAndShareSelected(context, selectedIds)
                        selectedIds = emptySet()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Delen (${selectedIds.size})") }
                Button(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Verwijder (${selectedIds.size})") }
            } else {
                if (activeDrive != null) {
                    Button(
                        onClick = { viewModel.endDrive() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Beëindig rit") }
                } else {
                    Button(
                        onClick = { showStartDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Start rit") }
                }
                OutlinedButton(
                    onClick = { showExportSheet = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Import / Export") }
            }
        }

        if (viewModel.nextcloudConfigured) {
            SyncStatusRow(
                syncStatus = syncStatus,
                onSyncNow = { viewModel.syncToNextcloud() }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (drives.isEmpty()) {
            Text(
                text = stringResource(R.string.no_drives_yet),
                modifier = Modifier.padding(top = 32.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(drives, key = { it.id }) { drive ->
                    DriveCard(
                        drive = drive,
                        selected = drive.id in selectedIds,
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (drive.id in selectedIds)
                                    selectedIds - drive.id else selectedIds + drive.id
                            } else {
                                onDriveClick(drive.id)
                            }
                        },
                        onLongClick = {
                            selectedIds = selectedIds + drive.id
                        }
                    )
                }
            }
        }
    }
}

// ── Start drive dialog ────────────────────────────────────────────────────────

@Composable
private fun StartDriveDialog(
    canContinue: Boolean,
    onBusiness: () -> Unit,
    onPrivate: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Type rit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canContinue) {
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text("Hervat vorige rit")
                    }
                    HorizontalDivider()
                }
                Button(onClick = onBusiness, modifier = Modifier.fillMaxWidth()) {
                    Text("Zakelijk")
                }
                OutlinedButton(onClick = onPrivate, modifier = Modifier.fillMaxWidth()) {
                    Text("Privé")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}

// ── Export bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    drives: List<Drive>,
    nextcloudConfigured: Boolean,
    onDismiss: () -> Unit,
    onExport: (from: Long?, to: Long?) -> Unit,
    onImportFromNextcloud: () -> Unit,
    onImport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPeriod by remember { mutableStateOf(Period.MONTH) }
    var customFrom by remember { mutableStateOf<Long?>(null) }
    var customTo by remember { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val range = remember(selectedPeriod, customFrom, customTo) {
        periodToRange(selectedPeriod, customFrom, customTo)
    }
    val completedDrives = remember(drives) { drives.filter { !it.isActive } }
    val filteredDrives = remember(completedDrives, range) {
        if (range == null) completedDrives
        else completedDrives.filter { it.startTime in range.first..range.second }
    }
    val businessKm = remember(filteredDrives) {
        filteredDrives.filter { it.driveType == DriveType.BUSINESS }.sumOf { it.kmDriven ?: 0 }
    }
    val privateKm = remember(filteredDrives) {
        filteredDrives.filter { it.driveType == DriveType.PRIVATE }.sumOf { it.kmDriven ?: 0 }
    }

    if (showDateRangePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        customFrom = utcMidnightToLocalStart(start)
                        customTo = utcMidnightToLocalEnd(end)
                        selectedPeriod = Period.CUSTOM
                    }
                    showDateRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Annuleren") }
            }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Exporteer ritten", style = MaterialTheme.typography.titleLarge)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Period.entries) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = {
                            if (period == Period.CUSTOM) showDateRangePicker = true
                            else selectedPeriod = period
                        },
                        label = { Text(period.label) }
                    )
                }
            }

            SummaryCard(businessKm = businessKm, privateKm = privateKm, count = filteredDrives.size)

            Button(
                onClick = { onExport(range?.first, range?.second) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exporteer ${filteredDrives.size} ritten als CSV")
            }

            HorizontalDivider()

            Text("Ritten herstellen", style = MaterialTheme.typography.titleMedium)
            Text(
                "Importeer een eerder geëxporteerde CSV om verwijderde ritten terug te zetten. Bestaande ritten worden overgeslagen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (nextcloudConfigured) {
                Button(onClick = onImportFromNextcloud, modifier = Modifier.fillMaxWidth()) {
                    Text("Herstel van Nextcloud")
                }
            }
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("Importeer lokaal CSV-bestand")
            }
        }
    }
}

// ── Sync status row ───────────────────────────────────────────────────────────

@Composable
private fun SyncStatusRow(syncStatus: SyncStatus, onSyncNow: () -> Unit) {
    val timeFmt = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale("nl", "NL")) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (statusText, statusColor) = when (syncStatus) {
            SyncStatus.Idle -> "Nog niet gesynchroniseerd" to MaterialTheme.colorScheme.onSurfaceVariant
            SyncStatus.Syncing -> "Synchroniseren…" to MaterialTheme.colorScheme.onSurfaceVariant
            is SyncStatus.Success -> "Gesynchroniseerd om ${timeFmt.format(java.util.Date(syncStatus.timestamp))}" to MaterialTheme.colorScheme.primary
            is SyncStatus.Error -> "Sync mislukt: ${syncStatus.message}" to MaterialTheme.colorScheme.error
        }
        Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor, modifier = Modifier.weight(1f))
        if (syncStatus !is SyncStatus.Syncing) {
            TextButton(onClick = onSyncNow, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("Sync nu", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(businessKm: Int, privateKm: Int, count: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Zakelijk", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "$businessKm km",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Privé: $privateKm km", style = MaterialTheme.typography.bodyMedium)
                Text("Ritten: $count", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ── Drive card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DriveCard(
    drive: Drive,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val fmt = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("nl", "NL"))
    val cardColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        drive.hasKmMismatch -> MismatchRed.copy(alpha = 0.15f)
        else -> Color.Unspecified
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (drive.driveType == DriveType.BUSINESS) "Zakelijk" else "Privé",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (drive.isActive) "Actief" else drive.kmDriven?.let { "$it km" } ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(fmt.format(Date(drive.startTime)), style = MaterialTheme.typography.bodyMedium)
            if (!drive.isActive && drive.endTime != null) {
                Text("→ ${fmt.format(Date(drive.endTime))}", style = MaterialTheme.typography.bodyMedium)
            }
            if (drive.startKm != null) {
                Text("KM: ${drive.startKm} → ${drive.endKm ?: "…"}", style = MaterialTheme.typography.bodySmall)
            }
            if (drive.hasKmMismatch) {
                Text(
                    text = "⚠ ${stringResource(R.string.km_mismatch_warning)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MismatchRed
                )
            }
        }
    }
}
