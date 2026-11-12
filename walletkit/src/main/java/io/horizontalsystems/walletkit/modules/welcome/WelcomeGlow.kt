package io.horizontalsystems.walletkit.modules.welcome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The glow at the bottom of the welcome screen. It rises from under the
 * screen, swells high once on the way, settles back to the bottom and then
 * breathes. It keeps its own time and does not follow what appears on the
 * screen above it.
 *
 * What it draws depends on the elapsed time only, and the time is read while
 * drawing, so a frame costs a redraw and no recomposition.
 *
 * @param animate false draws the settled glow and leaves it still.
 * @param startAt the time the glow starts from, [GLOW_SETTLED] to skip its
 * rise and opening pulse.
 */
@Composable
internal fun WelcomeGlow(animate: Boolean, startAt: Float, modifier: Modifier = Modifier) {
    val time = remember { mutableFloatStateOf(startAt) }
    if (animate) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { time.floatValue = startAt + (it - start) / 1_000_000_000f }
            }
        }
    }

    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                // The glow is drawn for a 393 wide screen and grows with the width
                val unit = size.width / DESIGN_WIDTH
                val brushes = GlowLayers.map { it.brush(it.outerRadius * unit) }

                onDrawBehind {
                    val t = time.floatValue
                    val rise = 1f - (1f - ((t - RISE_START) / RISE_DURATION).coerceIn(0f, 1f)).pow(3)
                    val energy = pulseEnergy(t)

                    GlowLayers.forEachIndexed { index, layer ->
                        // -1 breathed out, 1 breathed in
                        val breath = sin(TWO_PI * t / BREATH - layer.phase)
                        val scale = (0.55f + 0.45f * rise) *
                                (1f + layer.scaleAmplitude * breath * rise) *
                                (1f + PULSE_SCALE * layer.pulseGain * energy)
                        val lift = layer.lift * breath * rise +
                                PULSE_LIFT * layer.pulseGain * energy -
                                (1f - rise) * RISE_DISTANCE
                        val drift = layer.drift * rise *
                                sin(TWO_PI * t / layer.driftPeriod + 2f * layer.phase)
                        val alpha = min(1f, layer.opacity * rise * (1f - 0.05f * (1f - breath)))

                        withTransform({
                            translate(
                                left = size.width / 2f + drift * unit,
                                top = size.height - (layer.centerAboveBottom + lift) * unit,
                            )
                            scale(scale, scale, pivot = Offset.Zero)
                        }) {
                            drawCircle(
                                brush = brushes[index],
                                radius = layer.outerRadius * unit,
                                center = Offset.Zero,
                                alpha = alpha,
                            )
                        }
                    }
                }
            }
    )
}

/**
 * One blurred disc of the glow, measured on the 393 wide design frame.
 * Blurring every frame is costly and looks different across Android
 * versions, so each disc is a radial gradient with the falloff of its blur.
 */
private class GlowLayer(
    val color: Color,
    /** How far above the bottom edge the centre rests. Negative is below the screen. */
    val centerAboveBottom: Float,
    /** The disc's radius plus three widths of its blur, where it has faded out. */
    val outerRadius: Float,
    /** Alpha of the blurred disc in even steps from its centre to [outerRadius]. */
    val alphaStops: FloatArray,
    val opacity: Float,
    /** How much a breath scales the disc, lifts it and moves it sideways. */
    val scaleAmplitude: Float,
    val lift: Float,
    val drift: Float,
    /** Seconds for one sideways swing. */
    val driftPeriod: Float,
    /** How far this disc lags within a breath, in radians. */
    val phase: Float,
    /** How strongly the disc answers the opening pulse. */
    val pulseGain: Float,
) {
    fun brush(radius: Float): Brush {
        val last = (alphaStops.size - 1).toFloat()
        val stops = Array(alphaStops.size) { it / last to color.copy(alpha = alphaStops[it]) }
        return Brush.radialGradient(*stops, center = Offset.Zero, radius = radius)
    }
}

// Back to front
private val GlowLayers = listOf(
    GlowLayer(
        color = Color(0xFFFF0000),
        centerAboveBottom = 93f,
        outerRadius = 640f,
        alphaStops = floatArrayOf(0.5517f, 0.5294f, 0.4673f, 0.3787f, 0.2808f, 0.1897f, 0.1163f, 0.0644f, 0.0322f, 0.0144f, 0.0058f, 0.0021f, 0f),
        opacity = 0.6f,
        scaleAmplitude = 0.07f,
        lift = 18f,
        drift = 10f,
        driftPeriod = 11f,
        phase = 0.0f,
        pulseGain = 0.6f,
    ),
    GlowLayer(
        color = Color(0xFFFFBB00),
        centerAboveBottom = -61f,
        outerRadius = 280f,
        alphaStops = floatArrayOf(0.9660f, 0.9530f, 0.9091f, 0.8236f, 0.6911f, 0.5230f, 0.3483f, 0.2000f, 0.0975f, 0.0399f, 0.0135f, 0.0038f, 0f),
        opacity = 1.0f,
        scaleAmplitude = 0.16f,
        lift = 34f,
        drift = 22f,
        driftPeriod = 13f,
        phase = 1.4f,
        pulseGain = 1.3f,
    ),
    GlowLayer(
        color = Color(0xFFFE4A11),
        centerAboveBottom = 14f,
        outerRadius = 460f,
        alphaStops = floatArrayOf(0.7220f, 0.6962f, 0.6229f, 0.5143f, 0.3888f, 0.2669f, 0.1650f, 0.0912f, 0.0448f, 0.0194f, 0.0074f, 0.0025f, 0f),
        opacity = 0.8f,
        scaleAmplitude = 0.10f,
        lift = 26f,
        drift = 16f,
        driftPeriod = 9f,
        phase = 0.7f,
        pulseGain = 0.9f,
    ),
)

/** How much the opening pulse swells the glow at [time]: it rises softly and fades slowly. */
private fun pulseEnergy(time: Float): Float {
    if (time <= PULSE_START) return 0f

    val since = time - PULSE_START
    return PULSE_STRENGTH * PULSE_RELEASE / (PULSE_RELEASE - PULSE_ATTACK) *
            (exp(-since / PULSE_RELEASE) - exp(-since / PULSE_ATTACK))
}

/** A moment by which the glow has risen, its opening pulse has faded and it breathes evenly. */
internal const val GLOW_SETTLED = 10.4f

private const val DESIGN_WIDTH = 393f
private const val TWO_PI = (2 * PI).toFloat()

/** Seconds per breath. */
private const val BREATH = 5.2f

// The rise from under the screen: when it starts, how long it takes and from how far below
private const val RISE_START = 0.1f
private const val RISE_DURATION = 1.8f
private const val RISE_DISTANCE = 240f

// The opening pulse: when it starts, how strong it is, seconds to swell and
// to fade, and how much it scales and lifts the discs
private const val PULSE_START = 0.1f
private const val PULSE_STRENGTH = 2.0f
private const val PULSE_ATTACK = 0.6f
private const val PULSE_RELEASE = 1.4f
private const val PULSE_SCALE = 0.16f
private const val PULSE_LIFT = 46f
