package com.rykle.kmregistratie.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveStats
import com.rykle.kmregistratie.domain.model.DriveTotals
import com.rykle.kmregistratie.domain.model.Project
import com.rykle.kmregistratie.domain.model.ProjectLocations
import com.rykle.kmregistratie.ui.theme.Blue
import com.rykle.kmregistratie.ui.theme.GlassCard
import com.rykle.kmregistratie.ui.theme.Green
import com.rykle.kmregistratie.ui.theme.Line
import com.rykle.kmregistratie.ui.theme.RevealEasing
import com.rykle.kmregistratie.ui.theme.reveal
import com.rykle.kmregistratie.ui.theme.shineBrush
import com.rykle.kmregistratie.ui.viewmodel.DriveViewModel
import java.time.LocalDate

// Kleuren van de reeksen (gevalideerd tegen de donkere kaart): groen = zakelijk, blauw = privé.
private val BusinessColor = Green
private val PrivateColor = Blue


@Composable
fun OverviewScreen(viewModel: DriveViewModel, onDriveClick: (Long) -> Unit = {}) {
    val drives by viewModel.drives.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val home by viewModel.home.collectAsState()
    val completed = remember(drives) { drives.filter { !it.isActive } }

    var granularity by rememberSaveable { mutableStateOf(OverviewGranularity.YEAR) }
    var anchorDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val period = OverviewPeriod(granularity, LocalDate.ofEpochDay(anchorDay))

    val inPeriod = remember(completed, period) { completed.filter { period.contains(it.startTime) } }
    val totals = remember(inPeriod) { DriveStats.totals(inPeriod) }
    val perProject = remember(inPeriod) { DriveStats.perProject(inPeriod) }
    // De 500 km-grens geldt per kalenderjaar: altijd het jaar waarin de periode valt.
    val meterYear = period.start.year
    val privateKmInYear = remember(completed, meterYear) {
        DriveStats.totals(completed.filter { DriveStats.year(it) == meterYear }).privateKm
    }
    val currentYear = LocalDate.now().year
    val buckets = remember(period, completed) {
        val years = completed.map { DriveStats.year(it) }
        period.buckets(years.minOrNull() ?: currentYear, maxOf(years.maxOrNull() ?: currentYear, currentYear))
    }

    var editProject by remember { mutableStateOf<Project?>(null) }
    var nearbyFor by remember { mutableStateOf<Project?>(null) }
    editProject?.let { p ->
        EditProjectDialog(
            project = p,
            nearbyCount = remember(p, drives) {
                ProjectLocations.nearbyDrives(p, drives, home).count { it.drive.projectId == null }
            },
            onDismiss = { editProject = null },
            onSave = { name, client -> viewModel.updateProjectDetails(p.id, name, client); editProject = null },
            onToggleArchived = { viewModel.setProjectArchived(p.id, !p.archived); editProject = null },
            onFindNearby = { nearbyFor = p; editProject = null },
        )
    }
    nearbyFor?.let { p ->
        NearbyDrivesDialog(
            project = p,
            drives = drives,
            projectNames = projects.associate { it.id to it.name },
            home = home,
            onDismiss = { nearbyFor = null },
            onLink = { ids, rejected -> viewModel.linkDrivesToProject(ids, p.id, rejected); nearbyFor = null },
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item(key = "title") {
            Text("Overzicht", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.reveal(0))
        }

        item(key = "period") {
            Column(Modifier.padding(top = 8.dp).reveal(1)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(OverviewGranularity.entries) { g ->
                        FilterChip(
                            selected = granularity == g,
                            onClick = { granularity = g },
                            label = { Text(g.label) },
                        )
                    }
                }
                PeriodNavigator(
                    period = period,
                    onPrevious = { anchorDay = period.previous().anchor.toEpochDay() },
                    onNext = { anchorDay = period.next().anchor.toEpochDay() },
                    onToday = { anchorDay = LocalDate.now().toEpochDay() },
                )
            }
        }

        item(key = "totals") {
            GlassCardBox(Modifier.reveal(2)) {
                Text("Zakelijk", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${totals.businessKm} km",
                    style = MaterialTheme.typography.displaySmall.copy(brush = shineBrush()),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Privé", "${totals.privateKm} km")
                    Stat("Ritten", totals.drives.toString())
                    Stat("Rijtijd", formatHours(totals.drivingMillis))
                }
                if (totals.unknownKmDrives > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${totals.unknownKmDrives} ritten zonder bekende eindstand tellen niet mee in de km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (granularity != OverviewGranularity.ALL) {
                    Spacer(Modifier.height(12.dp))
                    PrivateKmMeter(privateKmInYear, year = meterYear)
                }
            }
        }

        if (buckets != null) {
            item(key = "chart") {
                ChartCard(
                    buckets = buckets,
                    drives = inPeriod,
                    title = when (granularity) {
                        OverviewGranularity.YEAR -> "Km per maand"
                        OverviewGranularity.ALL -> "Km per jaar"
                        else -> "Km per dag"
                    },
                    modifier = Modifier.padding(top = 12.dp).reveal(3),
                )
            }
        } else {
            item(key = "day-title") {
                Text(
                    if (inPeriod.isEmpty()) "Geen ritten op deze dag" else "Ritten",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
            }
            items(inPeriod.sortedBy { it.startTime }, key = { "drive-${it.id}" }) { d ->
                DayDriveRow(d, projects.firstOrNull { it.id == d.projectId }?.name, onClick = { onDriveClick(d.id) })
            }
        }

        item(key = "projects-title") {
            Text(
                "Projecten",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).reveal(4),
            )
        }
        if (perProject.isEmpty()) {
            item(key = "no-projects") {
                Text(
                    "Geen ritten in deze periode. Koppel een rit aan een project bij het starten of in het ritdetail.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(perProject, key = { "project-${it.first}" }) { (projectId, t) ->
            val project = projects.firstOrNull { it.id == projectId }
            ProjectRow(
                name = project?.name ?: "Zonder project",
                archived = project?.archived == true,
                totals = t,
                maxKm = perProject.maxOf { it.second.totalKm }.coerceAtLeast(1),
                onClick = project?.let { { editProject = it } },
                modifier = Modifier.reveal(5),
            )
        }
        // Projecten zonder ritten in deze periode, zodat je ze toch kunt beheren.
        val unused = projects.filter { p -> perProject.none { it.first == p.id } }
        if (unused.isNotEmpty()) {
            item(key = "unused") {
                Text(
                    "Geen ritten in deze periode: " + unused.joinToString { it.name + if (it.archived) " (gearchiveerd)" else "" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PeriodNavigator(period: OverviewPeriod, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        IconButton(onClick = onPrevious, enabled = period.hasPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Vorige periode")
        }
        // Tik op de titel om terug te springen naar vandaag.
        Text(
            period.title.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f).clickable(onClick = onToday),
        )
        IconButton(onClick = onNext, enabled = period.hasNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Volgende periode")
        }
    }
}

@Composable
private fun DayDriveRow(drive: Drive, projectName: String?, onClick: () -> Unit) {
    val fmt = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale("nl", "NL")) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, Line),
        colors = CardDefaults.cardColors(containerColor = GlassCard, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .then(Modifier.padding(0.dp))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(if (drive.driveType == com.rykle.kmregistratie.domain.model.DriveType.BUSINESS) BusinessColor else PrivateColor)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${fmt.format(java.util.Date(drive.startTime))} – ${drive.endTime?.let { fmt.format(java.util.Date(it)) } ?: "…"}",
                    style = MaterialTheme.typography.titleSmall,
                )
                val detail = listOfNotNull(projectName, drive.note, drive.endAddress).joinToString(" · ")
                if (detail.isNotEmpty()) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Text(drive.kmDriven?.let { "$it km" } ?: "? km", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun GlassCardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Line),
        colors = CardDefaults.cardColors(containerColor = GlassCard, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

// ── Grafiek ───────────────────────────────────────────────────────────────────

@Composable
private fun ChartCard(buckets: List<Bucket>, drives: List<Drive>, title: String, modifier: Modifier = Modifier) {
    val perBucket = remember(buckets, drives) {
        buckets.map { b -> DriveStats.totals(drives.filter { it.startTime >= b.from && it.startTime < b.to }) }
    }
    val now = System.currentTimeMillis()
    var selected by remember(buckets) {
        mutableStateOf(
            buckets.indexOfFirst { now >= it.from && now < it.to }.takeIf { it >= 0 }
                ?: perBucket.indexOfLast { it.drives > 0 }.coerceAtLeast(0)
        )
    }
    var showTable by rememberSaveable { mutableStateOf(false) }

    GlassCardBox(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            LegendDot(BusinessColor, "Zakelijk")
            Spacer(Modifier.width(12.dp))
            LegendDot(PrivateColor, "Privé")
        }
        Spacer(Modifier.height(12.dp))
        Bars(buckets, perBucket, selected = selected, onSelect = { selected = it })
        Spacer(Modifier.height(12.dp))
        val t = perBucket[selected]
        Text(buckets[selected].long.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
        Text(
            if (t.drives == 0) "Geen ritten"
            else "${t.businessKm} km zakelijk · ${t.privateKm} km privé · ${t.drives} ritten · ${formatHours(t.drivingMillis)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { showTable = !showTable }, contentPadding = PaddingValues(0.dp)) {
            Text(if (showTable) "Tabel verbergen" else "Als tabel tonen")
        }
        if (showTable) BucketTable(buckets, perBucket)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape)) {
            Canvas(Modifier.fillMaxSize()) { drawCircle(color) }
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Gestapelde staven: zakelijk onder, privé erboven, 2dp ruimte tussen de delen en afgeronde
 * bovenkant. Tik op een staaf (of schuif eroverheen) voor een tooltip met de waarden, in de stijl
 * van de grafieken in eems; nogmaals tikken op dezelfde staaf sluit hem.
 */
@Composable
private fun Bars(buckets: List<Bucket>, perBucket: List<DriveTotals>, selected: Int, onSelect: (Int) -> Unit) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(900, easing = RevealEasing))
    }
    var tipVisible by remember(buckets) { mutableStateOf(false) }
    val tipAlpha by animateFloatAsState(if (tipVisible) 1f else 0f, tween(120, easing = RevealEasing), label = "tip")
    val n = buckets.size
    val maxKm = perBucket.maxOf { it.totalKm }.coerceAtLeast(1)
    val gridColor = Line
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectColor = Color.White.copy(alpha = 0.06f)
    val labelStyle = MaterialTheme.typography.labelSmall
    val chartHeight = 160.dp

    Column {
        BoxWithConstraints(Modifier.fillMaxWidth().height(chartHeight)) {
            val widthPx = constraints.maxWidth.toFloat()
            fun indexAt(x: Float) = (x / (widthPx / n)).toInt().coerceIn(0, n - 1)

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(buckets) {
                        detectTapGestures { pos ->
                            val i = indexAt(pos.x)
                            tipVisible = !(tipVisible && i == selected)
                            onSelect(i)
                        }
                    }
                    // Horizontaal schuiven = scrubben; verticaal blijft de pagina scrollen.
                    .pointerInput(buckets) {
                        detectHorizontalDragGestures(
                            onDragStart = { pos -> tipVisible = true; onSelect(indexAt(pos.x)) },
                        ) { change, _ -> onSelect(indexAt(change.position.x)) }
                    }
            ) {
                val slot = size.width / n
                // Smalle staven, maar bij 31 dagen niet dunner dan een paar pixels.
                val barW = (slot * if (n > 12) 0.7f else 0.56f).coerceAtLeast(2.dp.toPx())
                val gap = 2.dp.toPx()
                val radius = (if (n > 12) 2.dp else 4.dp).toPx()

                // Recessieve hulplijnen op 50% en 100% van het maximum
                listOf(0.5f, 1f).forEach { f ->
                    val y = size.height * (1 - f)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                }
                drawRect(selectColor, Offset(selected * slot, 0f), Size(slot, size.height))

                perBucket.forEachIndexed { i, t ->
                    val x = i * slot + (slot - barW) / 2
                    val bH = size.height * t.businessKm / maxKm * grow.value
                    val pH = size.height * t.privateKm / maxKm * grow.value
                    val dim = if (i == selected) 1f else 0.75f
                    if (bH > 0) {
                        roundedTopBar(BusinessColor.copy(alpha = dim), x, size.height - bH, barW, bH, if (pH > 0) 0f else radius)
                    }
                    if (pH > 0) {
                        val top = size.height - bH - (if (bH > 0) gap else 0f) - pH
                        roundedTopBar(PrivateColor.copy(alpha = dim), x, top, barW, pH, radius)
                    }
                }
            }

            if (tipAlpha > 0f) {
                var tipWidth by remember { mutableStateOf(0) }
                val density = LocalDensity.current
                val slot = widthPx / n
                val margin = with(density) { 8.dp.toPx() }
                // Rechts van de staaf; past dat niet, dan links ervan (zoals in eems).
                val right = (selected + 1) * slot + margin
                val x = if (right + tipWidth > widthPx) selected * slot - margin - tipWidth else right
                ChartTooltip(
                    title = buckets[selected].long.replaceFirstChar { it.uppercase() },
                    totals = perBucket[selected],
                    modifier = Modifier
                        .onSizeChanged { tipWidth = it.width }
                        .offset { IntOffset(x.coerceAtLeast(0f).toInt(), 0) }
                        .graphicsLayer { alpha = tipAlpha },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { i, b ->
                Text(
                    b.short,
                    style = labelStyle,
                    color = if (i == selected) MaterialTheme.colorScheme.onSurface else labelColor,
                    softWrap = false,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                    modifier = Modifier.weight(1f).clickable { tipVisible = true; onSelect(i) },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

// eems: .chart-tip (rgba(15,23,42,.92), rand #2b3a5a), .chart-tip-time, .chart-tip-row
private val TipBackground = Color(0xEB0F172A)
private val TipBorder = Color(0xFF2B3A5A)
private val TipDim = Color(0xFF94A3B8)

@Composable
private fun ChartTooltip(title: String, totals: DriveTotals, modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(min = 150.dp)
            .width(IntrinsicSize.Max)
            .shadow(12.dp, RoundedCornerShape(10.dp))
            .background(TipBackground, RoundedCornerShape(10.dp))
            .border(1.dp, TipBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TipDim)
        Spacer(Modifier.height(4.dp))
        if (totals.drives == 0) {
            Text("Geen ritten", style = MaterialTheme.typography.bodySmall, color = TipDim)
        } else {
            // Gestapeld: bovenste deel (privé) eerst, zoals in eems.
            TipRow(PrivateColor, "Privé", "${totals.privateKm} km")
            TipRow(BusinessColor, "Zakelijk", "${totals.businessKm} km")
            TipRow(null, "Totaal", "${totals.totalKm} km")
            TipRow(null, "Ritten", totals.drives.toString())
            TipRow(null, "Rijtijd", formatHours(totals.drivingMillis))
        }
    }
}

@Composable
private fun TipRow(dot: Color?, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Box(Modifier.size(8.dp)) {
            if (dot != null) Canvas(Modifier.fillMaxSize()) { drawCircle(dot) }
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TipDim)
        Spacer(Modifier.weight(1f).widthIn(min = 12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

/** Staaf met alleen de bovenhoeken afgerond (onderkant staat op de basislijn). */
private fun DrawScope.roundedTopBar(color: Color, x: Float, y: Float, w: Float, h: Float, radius: Float) {
    val r = radius.coerceAtMost(h).coerceAtMost(w / 2)
    val path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = x, top = y, right = x + w, bottom = y + h,
                topLeftCornerRadius = CornerRadius(r, r),
                topRightCornerRadius = CornerRadius(r, r),
                bottomLeftCornerRadius = CornerRadius.Zero,
                bottomRightCornerRadius = CornerRadius.Zero,
            )
        )
    }
    drawPath(path, color)
}

@Composable
private fun BucketTable(buckets: List<Bucket>, perBucket: List<DriveTotals>) {
    Column {
        TableRow("Periode", "Zakelijk", "Privé", "Ritten", "Uren", header = true)
        HorizontalDivider(color = Line)
        buckets.zip(perBucket).filter { it.second.drives > 0 }.forEach { (b, t) ->
            TableRow(
                b.long,
                "${t.businessKm}",
                "${t.privateKm}",
                "${t.drives}",
                "%.1f".format(t.drivingHours),
            )
        }
    }
}

@Composable
private fun TableRow(vararg cells: String, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        cells.forEachIndexed { i, c ->
            Text(
                c,
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = if (i == 0) androidx.compose.ui.text.style.TextAlign.Start else androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(if (i == 0) 2f else 1f),
            )
        }
    }
}

// ── Projecten ─────────────────────────────────────────────────────────────────

@Composable
private fun ProjectRow(
    name: String,
    archived: Boolean,
    totals: DriveTotals,
    maxKm: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        border = BorderStroke(1.dp, Line),
        colors = CardDefaults.cardColors(containerColor = GlassCard, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name + if (archived) " (gearchiveerd)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("${totals.totalKm} km", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${formatHours(totals.drivingMillis)} rijtijd · ${totals.drives} ritten" +
                    if (totals.privateKm > 0) " · ${totals.privateKm} km privé" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            // Aandeel t.o.v. het grootste project: één kleur, want het is een hoeveelheid.
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                drawRoundRect(Line, cornerRadius = CornerRadius(size.height / 2))
                drawRoundRect(
                    BusinessColor,
                    size = Size(size.width * totals.totalKm / maxKm, size.height),
                    cornerRadius = CornerRadius(size.height / 2),
                )
            }
        }
    }
}

@Composable
private fun EditProjectDialog(
    project: Project,
    nearbyCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, client: String?) -> Unit,
    onToggleArchived: () -> Unit,
    onFindNearby: () -> Unit,
) {
    var name by remember { mutableStateOf(project.name) }
    var client by remember { mutableStateOf(project.client.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Klant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (project.hasLocation) {
                    Text(
                        "Locatie: ${project.address ?: "GPS-punt"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onFindNearby, modifier = Modifier.fillMaxWidth()) {
                        Text(if (nearbyCount > 0) "$nearbyCount ritten op deze plek koppelen" else "Geen nieuwe ritten op deze plek")
                    }
                } else {
                    Text(
                        "Geen locatie. Leg er een vast via een rit: ritdetail → \"Locatie opslaan als project\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onToggleArchived, modifier = Modifier.fillMaxWidth()) {
                    Text(if (project.archived) "Terugzetten uit archief" else "Archiveren (verbergen bij starten)")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, client) }, enabled = name.isNotBlank()) { Text("Opslaan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}
