package com.rykle.kmregistratie.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.rykle.kmregistratie.domain.model.DriveStats
import com.rykle.kmregistratie.domain.model.DriveTotals
import com.rykle.kmregistratie.domain.model.Project
import com.rykle.kmregistratie.domain.model.ProjectLocations
import com.rykle.kmregistratie.ui.theme.Green2
import com.rykle.kmregistratie.ui.theme.RevealEasing
import com.rykle.kmregistratie.ui.theme.Warning
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rykle.kmregistratie.R
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.ui.theme.MismatchRed
import com.rykle.kmregistratie.ui.theme.FlowDots
import com.rykle.kmregistratie.ui.theme.GlassCard
import com.rykle.kmregistratie.ui.theme.Green
import com.rykle.kmregistratie.ui.theme.GreenDark
import com.rykle.kmregistratie.ui.theme.Line
import com.rykle.kmregistratie.ui.theme.glow
import com.rykle.kmregistratie.ui.theme.pressLift
import com.rykle.kmregistratie.ui.theme.reveal
import com.rykle.kmregistratie.ui.theme.shineBrush
import com.rykle.kmregistratie.ui.theme.shineSweep
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveListScreen(viewModel: DriveViewModel, onDriveClick: (Long) -> Unit = {}) {
    val drives by viewModel.drives.collectAsState()
    val volvoStatus by viewModel.volvoStatus.collectAsState()
    val startDrive by viewModel.startDrive.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val projectNames = remember(projects) { projects.associate { it.id to it.name } }
    // Rit zonder project die bij een bekende projectlocatie begon of eindigde → voorstel op de kaart.
    val rejected by viewModel.rejectedSuggestions.collectAsState()
    val home by viewModel.home.collectAsState()
    val suggestions = remember(drives, projects, rejected, home) {
        drives.mapNotNull { d ->
            ProjectLocations.suggestFor(d, projects, rejected[d.id].orEmpty(), home)?.let { d.id to it }
        }.toMap()
    }
    var chooseProjectFor by remember { mutableStateOf<Drive?>(null) }
    val context = LocalContext.current

    var selectedPeriod by rememberSaveable { mutableStateOf(Period.MONTH) }
    var customFrom by rememberSaveable { mutableStateOf<Long?>(null) }
    var customTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val range = remember(selectedPeriod, customFrom, customTo) {
        periodToRange(selectedPeriod, customFrom, customTo)
    }
    val completedDrives = remember(drives) { drives.filter { !it.isActive } }
    // Zoeken kijkt door alle ritten heen; anders bepaalt de gekozen periode wat je ziet.
    val filteredDrives = remember(completedDrives, range, query, projectNames) {
        when {
            query.isNotBlank() -> completedDrives.filter { matchesSearch(it, query, projectNames) }
            range == null -> completedDrives
            else -> completedDrives.filter { it.startTime in range.first..range.second }
        }
    }
    val totals = remember(filteredDrives) { DriveStats.totals(filteredDrives) }
    val privateKmThisYear = remember(completedDrives) {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        DriveStats.totals(completedDrives.filter { DriveStats.year(it) == year }).privateKm
    }
    val activeDrive = drives.firstOrNull { it.isActive }
    // Alleen de kaarten die bij het openen in beeld staan komen animerend binnen;
    // wat je daarna in beeld scrolt staat er meteen, anders voelt scrollen traag.
    val openedAt = remember { System.currentTimeMillis() }

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
        StartDriveDialog(
            projects = recentProjectsFirst(projects, drives),
            onCreateProject = viewModel::createProject,
            onStart = { type, projectId -> viewModel.startDrive(type, projectId); showStartDialog = false },
            onDismiss = { showStartDialog = false }
        )
    }

    // ── Startstand ophalen / bevestigen ───────────────────────────────────
    startDrive?.let { state ->
        StartKmDialog(
            state = state,
            onConfirm = viewModel::confirmStartKm,
            onRetry = viewModel::retryStartKm,
            onCancel = viewModel::cancelStartDrive,
        )
    }

    // ── Ander project kiezen vanuit een voorstel ─────────────────────────
    chooseProjectFor?.let { drive ->
        ChooseProjectDialog(
            projects = recentProjectsFirst(projects, drives),
            onCreateProject = viewModel::createProject,
            onDismiss = { chooseProjectFor = null },
            onChoose = { projectId ->
                // Alleen het project wijzigt; de notitie blijft staan.
                viewModel.setDriveProjectAndNote(drive.id, projectId, drive.note)
                chooseProjectFor = null
            },
        )
    }

    // ── Export bottom sheet ───────────────────────────────────────────────
    if (showExportSheet) {
        ExportSheet(
            drives = drives,
            onDismiss = { showExportSheet = false },
            onExport = { from, to ->
                viewModel.exportAndShare(context, from, to)
                showExportSheet = false
            }
        )
    }

    // ── Main content ──────────────────────────────────────────────────────
    // Eén scrollende lijst: de kop scrolt mee weg, zodat er ruimte is voor de ritten.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.reveal(0)) {
                Text(
                    stringResource(R.string.screen_title_drives),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) query = ""
                }) {
                    Icon(
                        if (searching) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (searching) "Zoeken sluiten" else "Zoeken",
                    )
                }
            }
        }

        item(key = "search") {
            AnimatedVisibility(searching) {
                SearchField(query = query, onQueryChange = { query = it }, modifier = Modifier.padding(top = 8.dp))
            }
        }

        item(key = "status") {
            VolvoStatusBanner(
                status = volvoStatus,
                onRetry = viewModel::checkVolvoConnection,
                modifier = Modifier.padding(top = 12.dp).reveal(1),
            )
        }

        item(key = "periods") {
            if (query.isBlank()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp).reveal(2),
                ) {
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
            } else {
                Text(
                    "${filteredDrives.size} ritten gevonden in alle periodes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item(key = "summary") {
            SummaryCard(
                totals = totals,
                privateKmThisYear = privateKmThisYear,
                driving = activeDrive != null,
                modifier = Modifier.padding(top = 12.dp).reveal(3),
            )
        }

        item(key = "actions") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp).reveal(4),
            ) {
                val driveButton = remember { MutableInteractionSource() }
                val shape = ButtonDefaults.shape
                if (activeDrive != null) {
                    val error = MaterialTheme.colorScheme.error
                    Button(
                        onClick = { viewModel.endDrive() },
                        modifier = Modifier.weight(1f).glow(error, shape).clip(shape).shineSweep(driveButton),
                        interactionSource = driveButton,
                        colors = ButtonDefaults.buttonColors(containerColor = error)
                    ) { Text("Beëindig rit") }
                } else {
                    // .lp-btn: groen verloop met groene gloed eronder
                    Button(
                        onClick = { showStartDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .glow(Green, shape)
                            .clip(shape)
                            .background(Brush.linearGradient(listOf(Green, GreenDark)))
                            .shineSweep(driveButton),
                        interactionSource = driveButton,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                    ) { Text("Start rit") }
                }
                OutlinedButton(
                    onClick = { showExportSheet = true },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_export_csv)) }
            }
        }

        val shown = listOfNotNull(activeDrive.takeIf { query.isBlank() }) + filteredDrives
        if (shown.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = if (query.isNotBlank()) "Geen ritten gevonden voor \"$query\""
                           else stringResource(R.string.no_drives_yet),
                    modifier = Modifier.padding(top = 32.dp).reveal(5),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            itemsIndexed(shown, key = { _, drive -> drive.id }) { index, drive ->
                DriveCard(
                    drive = drive,
                    projectName = drive.projectId?.let { projectNames[it] },
                    suggestion = suggestions[drive.id],
                    onLinkSuggestion = { p -> viewModel.linkDrivesToProject(listOf(drive.id), p.id) },
                    onRejectSuggestion = { p -> viewModel.rejectSuggestion(drive.id, p.id) },
                    onChooseOtherProject = { chooseProjectFor = drive },
                    onClick = { onDriveClick(drive.id) },
                    modifier = Modifier.reveal(index + 5, enabled = System.currentTimeMillis() - openedAt < 500),
                )
            }
        }
    }
}

/** Alle woorden moeten ergens in de rit voorkomen: type, project, notitie, adres, datum of km. */
private fun matchesSearch(drive: Drive, query: String, projectNames: Map<Long, String>): Boolean {
    val fmt = SimpleDateFormat("dd-MM-yyyy", Locale("nl", "NL"))
    val haystack = listOfNotNull(
        if (drive.driveType == DriveType.BUSINESS) "zakelijk" else "privé prive",
        drive.projectId?.let { projectNames[it] },
        drive.note,
        drive.startAddress,
        drive.endAddress,
        fmt.format(Date(drive.startTime)),
        drive.startKm?.toString(),
        drive.endKm?.toString(),
        drive.kmDriven?.let { "$it km" },
        "mismatch".takeIf { drive.hasKmMismatch },
    ).joinToString(" ").lowercase()
    return query.lowercase().split(' ').filter { it.isNotBlank() }.all { it in haystack }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Zoek op adres, project, notitie, datum…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Wissen")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth().focusRequester(focus),
    )
}

// ── Start drive dialog ────────────────────────────────────────────────────────

@Composable
private fun StartDriveDialog(
    projects: List<Project>,
    onCreateProject: (String, (Project) -> Unit) -> Unit,
    onStart: (DriveType, projectId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var projectId by remember { mutableStateOf<Long?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rit starten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Project", style = MaterialTheme.typography.titleSmall)
                ProjectChooser(
                    projects = projects,
                    selectedId = projectId,
                    onSelect = { projectId = it },
                    onCreate = onCreateProject,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onStart(DriveType.BUSINESS, projectId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Zakelijk")
                }
                OutlinedButton(onClick = { onStart(DriveType.PRIVATE, projectId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Privé")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}

@Composable
private fun ChooseProjectDialog(
    projects: List<Project>,
    onCreateProject: (String, (Project) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onChoose: (Long?) -> Unit,
) {
    var projectId by remember { mutableStateOf<Long?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rit koppelen aan project") },
        text = {
            ProjectChooser(
                projects = projects,
                selectedId = projectId,
                onSelect = { projectId = it },
                onCreate = onCreateProject,
            )
        },
        confirmButton = { Button(onClick = { onChoose(projectId) }, enabled = projectId != null) { Text("Koppelen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

// ── Export bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    drives: List<Drive>,
    onDismiss: () -> Unit,
    onExport: (from: Long?, to: Long?) -> Unit,
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

            SummaryCard(totals = DriveStats.totals(filteredDrives))

            Button(
                onClick = { onExport(range?.first, range?.second) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exporteer ${filteredDrives.size} ritten als CSV")
            }
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    totals: DriveTotals,
    modifier: Modifier = Modifier,
    privateKmThisYear: Int? = null,
    driving: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Line),
        colors = CardDefaults.cardColors(containerColor = GlassCard, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Zakelijk", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${totals.businessKm} km",
                style = MaterialTheme.typography.displaySmall.copy(
                    brush = shineBrush()
                ),
                fontWeight = FontWeight.Bold,
            )
            FlowDots(active = driving, modifier = Modifier.padding(vertical = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Privé: ${totals.privateKm} km", style = MaterialTheme.typography.bodyMedium)
                Text("Ritten: ${totals.drives}", style = MaterialTheme.typography.bodyMedium)
                Text(formatHours(totals.drivingMillis), style = MaterialTheme.typography.bodyMedium)
            }
            if (privateKmThisYear != null) {
                Spacer(Modifier.height(12.dp))
                PrivateKmMeter(privateKmThisYear)
            }
        }
    }
}

/** Privé-km dit kalenderjaar tegen de 500 km-grens (geen bijtelling). */
@Composable
fun PrivateKmMeter(privateKm: Int, modifier: Modifier = Modifier, year: Int? = null) {
    val limit = DriveStats.PRIVATE_KM_LIMIT
    val fraction = privateKm.toFloat() / limit
    val color = when {
        fraction >= 1f -> MaterialTheme.colorScheme.error
        fraction >= 0.8f -> Warning
        else -> Green2
    }
    val animated by animateFloatAsState(fraction.coerceAtMost(1f), tween(1200, easing = RevealEasing), label = "meter")
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val thisYear = year == null || year == Calendar.getInstance().get(Calendar.YEAR)
            Text(if (thisYear) "Privé dit jaar" else "Privé in $year", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when {
                    privateKm > limit -> "⚠ $privateKm / $limit km — grens overschreden"
                    fraction >= 0.8f -> "⚠ $privateKm / $limit km — nog ${limit - privateKm} km"
                    else -> "$privateKm / $limit km"
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animated },
            color = color,
            trackColor = Line,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

fun formatHours(millis: Long): String {
    val minutes = millis / 60_000
    return "${minutes / 60}u ${"%02d".format(minutes % 60)}m"
}

// ── Drive card ────────────────────────────────────────────────────────────────

@Composable
private fun DriveCard(
    drive: Drive,
    projectName: String?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    suggestion: Project? = null,
    onLinkSuggestion: (Project) -> Unit = {},
    onRejectSuggestion: (Project) -> Unit = {},
    onChooseOtherProject: () -> Unit = {},
) {
    val fmt = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("nl", "NL"))
    val cardColor = if (drive.hasKmMismatch) MismatchRed.copy(alpha = 0.15f) else GlassCard

    val interaction = remember { MutableInteractionSource() }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pressLift(interaction),
        interactionSource = interaction,
        border = BorderStroke(1.dp, if (drive.hasKmMismatch) MismatchRed.copy(alpha = 0.5f) else Line),
        colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = MaterialTheme.colorScheme.onSurface)
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
                    text = when {
                        drive.isActive -> "Actief"
                        drive.kmPending -> "? km"
                        else -> drive.kmDriven?.let { "$it km" } ?: ""
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (projectName != null || drive.note != null) {
                Text(
                    listOfNotNull(projectName?.let { "▸ $it" }, drive.note).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(fmt.format(Date(drive.startTime)), style = MaterialTheme.typography.bodyMedium)
            if (!drive.isActive && drive.endTime != null) {
                Text("→ ${fmt.format(Date(drive.endTime))}", style = MaterialTheme.typography.bodyMedium)
            }
            if (drive.startKm != null) {
                val endKm = if (drive.kmPending) null else drive.endKm
                Text("KM: ${drive.startKm} → ${endKm ?: "…"}", style = MaterialTheme.typography.bodySmall)
            }
            if (drive.kmPending) {
                Text(
                    text = "⏳ Eindstand nog invullen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (suggestion != null) {
                Text(
                    "Hoort bij ${suggestion.name}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onRejectSuggestion(suggestion) }) { Text("Nee") }
                    TextButton(onClick = onChooseOtherProject) { Text("Ander project…") }
                    TextButton(onClick = { onLinkSuggestion(suggestion) }) { Text("Koppelen") }
                }
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
