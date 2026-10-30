package com.rykle.kmregistratie.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// Animatiestijl van baronpower.nl (landing.css / landing.js), vertaald naar Compose.

/** `cubic-bezier(.2,.7,.2,1)`: snel op gang, lang uitlopen. Voor verschijnen en optillen. */
val RevealEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/** `cubic-bezier(.34,1.56,.64,1)`: schiet iets door en veert terug. Voor iconen die opploppen. */
val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private const val REVEAL_MS = 900
private const val REVEAL_STAGGER_MS = 90

/** Systeeminstelling "Animaties uit" (de `prefers-reduced-motion` van de website). */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * `[data-reveal]`: element komt 28dp van onderen omhoog, vervaagt in en wordt scherp (blur 6dp → 0).
 * [index] geeft de vertraging (`--d`) zodat een rij elementen na elkaar binnenkomt.
 * [enabled] = false toont het element meteen, bv. voor een lijstkaart die al eens verschenen is.
 */
fun Modifier.reveal(index: Int = 0, enabled: Boolean = true): Modifier = composed {
    val reduced = rememberReducedMotion()
    val animate = enabled && !reduced
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            progress.animateTo(
                1f,
                tween(REVEAL_MS, delayMillis = index.coerceAtMost(8) * REVEAL_STAGGER_MS, easing = RevealEasing),
            )
        }
    }
    val offsetPx = with(LocalDensity.current) { 28.dp.toPx() }
    val blurPx = with(LocalDensity.current) { 6.dp.toPx() }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * offsetPx
        val blur = (1f - p) * blurPx
        // BlurEffect werkt vanaf Android 12; daaronder wordt hij genegeerd.
        renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
    }
}

/**
 * `.lp-btn::after`: een lichte glansstreep die schuin over de knop veegt (0,6s).
 * Op de website gebeurt dat bij hover; hier bij het verschijnen en bij elke tik.
 * Zet hem op dezelfde [interactionSource] als de knop, en clip de knop vooraf op zijn vorm.
 */
fun Modifier.shineSweep(interactionSource: InteractionSource): Modifier = composed {
    val reduced = rememberReducedMotion()
    val sweep = remember { Animatable(if (reduced) 1f else -0.2f) }
    LaunchedEffect(interactionSource) {
        if (reduced) return@LaunchedEffect
        launch { sweep.animateTo(1.2f, tween(600, delayMillis = 400)) }
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                launch {
                    sweep.snapTo(-0.2f)
                    sweep.animateTo(1.2f, tween(600))
                }
            }
        }
    }
    drawWithContent {
        drawContent()
        val p = sweep.value
        if (p <= -0.2f || p >= 1.2f) return@drawWithContent
        val band = size.width * 0.4f
        val x = -band + p * (size.width + band)
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.35f),
                1f to Color.Transparent,
                start = Offset(x, 0f),
                end = Offset(x + band, size.height * 0.6f),
            )
        )
    }
}

/** `.lp-pillar:hover { translateY(-6px) }`: kaart tilt zich op zolang je hem ingedrukt houdt. */
fun Modifier.pressLift(interactionSource: InteractionSource): Modifier = composed {
    val lift = remember { Animatable(0f) }
    val liftPx = with(LocalDensity.current) { 4.dp.toPx() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            val target = if (interaction is PressInteraction.Press) 1f else 0f
            launch { lift.animateTo(target, tween(350, easing = RevealEasing)) }
        }
    }
    graphicsLayer {
        translationY = -lift.value * liftPx
        shadowElevation = lift.value * liftPx * 2
    }
}

/**
 * `.lp-shine`: verloop dat continu door tekst schuift (8s per ronde).
 * Gebruik als `style = MaterialTheme.typography.x.copy(brush = shineBrush())`.
 */
@Composable
fun shineBrush(): Brush {
    val reduced = rememberReducedMotion()
    val shift by rememberInfiniteTransition(label = "shine").animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "shineShift",
    )
    // Zelfde verloop als op de site; herhaalt na de helft, zodat de lus naadloos aansluit (background-size: 200%).
    val stops = listOf(Green2, GreenPale, Blue2, Green2, GreenPale, Blue2, Green2)
    val width = 600f
    return Brush.linearGradient(
        colors = stops,
        start = Offset(-shift * width, 0f),
        end = Offset(-shift * width + width * 2, width * 0.18f),
        tileMode = TileMode.Repeated,
    )
}

private class AuroraBlob(val color: Color, val x: Float, val y: Float, val radius: Float, val periodMs: Int, val reverse: Boolean)

/**
 * `.lp-aurora`: drie zachte, langzaam zwevende gloedvlekken achter de inhoud (18s, 24s en 21s heen en weer).
 * Kleuren met lage alpha meegeven, de vlekken lopen zelf al uit naar transparant.
 */
fun Modifier.aurora(first: Color, second: Color, third: Color): Modifier = composed {
    val reduced = rememberReducedMotion()
    val blobs = remember(first, second, third) {
        listOf(
            AuroraBlob(first, 0.85f, 0.05f, 0.75f, 18_000, false),
            AuroraBlob(second, 0.05f, 0.25f, 0.65f, 24_000, true),
            AuroraBlob(third, 0.55f, 0.65f, 0.5f, 21_000, false),
        )
    }
    val transition = rememberInfiniteTransition(label = "aurora")
    val phases = blobs.map { transition.drift(it.periodMs, it.reverse, reduced) }
    drawBehind {
        blobs.forEachIndexed { i, blob ->
            // lp-drift: 0% (0,0) schaal 1 → 50% (+4,+3) schaal 1.12 → 100% (-3,-2) schaal .95
            val t = phases[i].value
            val (dx, dy, scale) = if (t < 0.5f) {
                val k = t * 2
                Triple(0.04f * k, 0.03f * k, 1f + 0.12f * k)
            } else {
                val k = (t - 0.5f) * 2
                Triple(0.04f - 0.07f * k, 0.03f - 0.05f * k, 1.12f - 0.17f * k)
            }
            val center = Offset(size.width * (blob.x + dx), size.height * (blob.y + dy))
            val radius = size.width * blob.radius * scale
            drawCircle(
                Brush.radialGradient(0f to blob.color, 0.65f to Color.Transparent, center = center, radius = radius),
                radius = radius,
                center = center,
            )
        }
    }
}

@Composable
private fun InfiniteTransition.drift(periodMs: Int, reverse: Boolean, reduced: Boolean): State<Float> = animateFloat(
    initialValue = if (reverse) 1f else 0f,
    targetValue = if (reduced) (if (reverse) 1f else 0f) else (if (reverse) 0f else 1f),
    animationSpec = infiniteRepeatable(
        tween(periodMs, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
        RepeatMode.Reverse,
    ),
    label = "drift",
)

/**
 * `.lp-grid-bg`: dun lijnenraster (48dp) dat langzaam diagonaal wegschuift (30s per vak)
 * en naar de randen toe wegvaagt.
 */
fun Modifier.techGrid(): Modifier = composed {
    val reduced = rememberReducedMotion()
    val shift by rememberInfiniteTransition(label = "grid").animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing)),
        label = "gridShift",
    )
    val cellPx = with(LocalDensity.current) { 48.dp.toPx() }
    val linePx = with(LocalDensity.current) { 1.dp.toPx() }
    drawBehind {
        val lineColor = Color.White.copy(alpha = 0.045f)
        val offset = shift * cellPx
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            var x = offset - cellPx
            while (x < size.width) {
                drawRect(lineColor, Offset(x, 0f), Size(linePx, size.height))
                x += cellPx
            }
            var y = offset - cellPx
            while (y < size.height) {
                drawRect(lineColor, Offset(0f, y), Size(size.width, linePx))
                y += cellPx
            }
            // mask-image: radial-gradient(ellipse at 60% 40%, #000 30%, transparent 80%)
            drawRect(
                Brush.radialGradient(
                    0.3f to Color.Black,
                    0.8f to Color.Transparent,
                    center = Offset(size.width * 0.6f, size.height * 0.4f),
                    radius = maxOf(size.width, size.height) * 0.7f,
                ),
                blendMode = BlendMode.DstIn,
            )
            canvas.restore()
        }
    }
}

/** Groene gloed onder een knop (`box-shadow: 0 8px 30px -8px rgba(58,170,53,.65)`). */
fun Modifier.glow(color: Color, shape: Shape): Modifier =
    shadow(elevation = 14.dp, shape = shape, ambientColor = color, spotColor = color)

/**
 * `.lp-links` + `.lp-particles`: stippellijn waarover gloeiende stippen stromen.
 * [active] = true: lijn lichter, stippen groen en sneller (er loopt een rit).
 */
@Composable
fun FlowDots(active: Boolean, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val period = if (active) 2_800 else 6_000
    val t by rememberInfiniteTransition(label = "flow").animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing)),
        label = "flowT",
    )
    val dotColors = if (active) listOf(Green2, GreenPale) else listOf(Blue2, Green2)
    val count = 6
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        val cy = size.height / 2
        val dash = 4.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = if (active) 0.28f else 0.12f),
            start = Offset(0f, cy),
            end = Offset(size.width, cy),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 2)),
        )
        val r = 3.dp.toPx()
        repeat(count) { i ->
            val pos = (t + i.toFloat() / count) % 1f
            val alpha = sin(pos * PI).toFloat()   // vaagt in en uit aan de uiteinden
            val c = Offset(pos * size.width, cy)
            val color = dotColors[i % dotColors.size]
            // drop-shadow(0 0 6px currentColor)
            drawCircle(
                Brush.radialGradient(0f to color.copy(alpha = 0.6f * alpha), 1f to Color.Transparent, center = c, radius = r * 3),
                radius = r * 3,
                center = c,
            )
            drawCircle(color.copy(alpha = alpha), radius = r, center = c)
        }
    }
}
