package com.spiritwisestudios.crossroadsoffate.ui.minigames

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameState
import com.spiritwisestudios.crossroadsoffate.minigames.games.LockPickingGame
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import kotlinx.coroutines.delay
import kotlin.math.*

// Arc geometry (canvas angles: 0=right, 90=bottom, 180=left, 270=top)
private const val TOP_ARC_START = 210f
private const val TOP_ARC_SWEEP = 120f
private const val BOTTOM_ARC_START = 30f
private const val BOTTOM_ARC_SWEEP = 120f

private const val ARC_THICKNESS_DP = 40f
private const val TOUCH_BAND_MULTIPLIER = 2.5f
private const val ANGLE_TOLERANCE = 15f

// How close the tension must be to the checkpoint to count as "reached"
private const val CHECKPOINT_TOLERANCE = 0.05f

/**
 * Fallout-style lock picking screen with multi-phase difficulty.
 *
 * Easy: 1 sweet spot, swipe bottom to end.
 * Medium: 2 sweet spots with a checkpoint at 50%.
 * Hard: 3 sweet spots with checkpoints at 33% and 66%.
 *
 * Both fingers must stay on their arcs at all times during multi-phase.
 * Lifting either finger triggers a slip (restart attempt, costs pick durability).
 */
@Composable
fun LockPickingScreen(
    gameState: MiniGameState,
    onLockPicked: () -> Unit,
    onPickSlipped: () -> Unit,
    onCancel: () -> Unit
) {
    // --- Game state from LockPickingGame ---
    @Suppress("UNCHECKED_CAST")
    val sweetSpots = gameState.getData<List<Float>>("sweetSpots") ?: listOf(0.5f)
    @Suppress("UNCHECKED_CAST")
    val checkpoints = gameState.getData<List<Float>>("checkpoints") ?: listOf(1f)
    val sweetSpotSize = gameState.getData<Float>("sweetSpotSize") ?: 0.15f
    val totalPhases = gameState.getData<Int>("totalPhases") ?: 1
    val pickDurability = gameState.getData<Int>("pickDurability") ?: LockPickingGame.MAX_PICK_USES
    val startTime = gameState.getData<Long>("startTime") ?: System.currentTimeMillis()
    val timeLimit = gameState.timeRemaining ?: 60
    val lastResult = gameState.getData<String>("lastResult")

    // --- Local interaction state ---
    // Current phase within this attempt (0-indexed, advances locally until all done)
    var localPhase by remember { mutableIntStateOf(0) }
    var pickAngle by remember { mutableFloatStateOf(0.5f) }
    var isPickTouching by remember { mutableStateOf(false) }
    var tensionProgress by remember { mutableFloatStateOf(0f) }
    var isTensionTouching by remember { mutableStateOf(false) }
    // Tracks the bottom arch floor — after reaching a checkpoint, tension can't go below it
    var tensionFloor by remember { mutableFloatStateOf(0f) }
    // Whether the current phase's sweet spot has been found (pick is in it)
    var phaseSpotFound by remember { mutableStateOf(false) }

    val currentSweetSpot = sweetSpots.getOrElse(localPhase) { 0.5f }
    val currentCheckpoint = checkpoints.getOrElse(localPhase) { 1f }

    val isInSweetSpot = isPickTouching &&
        abs(pickAngle - currentSweetSpot) <= sweetSpotSize / 2

    // Reset all local state when game state changes (new attempt after slip, or game restart)
    LaunchedEffect(sweetSpots, pickDurability) {
        localPhase = 0
        pickAngle = 0.5f
        tensionProgress = 0f
        tensionFloor = 0f
        isPickTouching = false
        isTensionTouching = false
        phaseSpotFound = false
    }

    // --- Haptic feedback ---
    val view = LocalView.current

    LaunchedEffect(isInSweetSpot) {
        if (isInSweetSpot) {
            delay(50) // debounce edge toggling
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            while (true) {
                delay(500)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    // --- Flash feedback ---
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    LaunchedEffect(lastResult, pickDurability) {
        when (lastResult) {
            "SUCCESS" -> {
                flashColor = Color.Green.copy(alpha = 0.25f)
                delay(400)
                flashColor = Color.Transparent
            }
            "SLIP" -> {
                flashColor = Color.Red.copy(alpha = 0.25f)
                delay(400)
                flashColor = Color.Transparent
            }
        }
    }

    // --- Timer ---
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            currentTimeMs = System.currentTimeMillis()
        }
    }
    val elapsed = ((currentTimeMs - startTime) / 1000).toInt()
    val remaining = maxOf(0, timeLimit - elapsed)
    val timerProgress = if (timeLimit > 0) remaining.toFloat() / timeLimit else 0f

    val hintText = when {
        totalPhases == 1 && !isPickTouching -> "Swipe the top arch to find the sweet spot"
        totalPhases == 1 && isInSweetSpot && !isTensionTouching -> "Hold steady! Swipe the bottom arch"
        totalPhases == 1 && isInSweetSpot && isTensionTouching -> "Keep going..."
        totalPhases == 1 -> "Keep searching..."
        // Multi-phase hints
        localPhase == 0 && !isPickTouching -> "Find the first position on the top arch"
        !isInSweetSpot && isPickTouching -> "Keep searching... don't lift your finger!"
        isInSweetSpot && !isTensionTouching -> "Found it! Swipe bottom arch to checkpoint"
        isInSweetSpot && isTensionTouching && tensionProgress < currentCheckpoint - CHECKPOINT_TOLERANCE ->
            "Keep swiping to the checkpoint..."
        isInSweetSpot && localPhase < totalPhases - 1 ->
            "Hold bottom! Slide top to next position"
        isInSweetSpot && isTensionTouching -> "Almost there... complete the swipe!"
        else -> "Don't let go!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // Flash overlay
        if (flashColor != Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize().background(flashColor))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Header ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Lock Picking",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Timer bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color(0xFF333333), RoundedCornerShape(7.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(timerProgress)
                            .background(
                                when {
                                    timerProgress > 0.5f -> Color.Green
                                    timerProgress > 0.25f -> Color.Yellow
                                    else -> Color.Red
                                },
                                RoundedCornerShape(7.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${remaining}s remaining",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Phase progress + durability indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (totalPhases > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Phase ${minOf(localPhase + 1, totalPhases)} of $totalPhases",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            repeat(totalPhases) { i ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(12.dp)
                                        .background(
                                            when {
                                                i < localPhase -> Color.Green
                                                i == localPhase -> Color.Yellow
                                                else -> Color(0xFF555555)
                                            },
                                            CircleShape
                                        )
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Picks: ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        repeat(LockPickingGame.MAX_PICK_USES) { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(14.dp)
                                    .background(
                                        if (i < pickDurability) Color(0xFF8B9DFF)
                                        else Color.Red,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            // --- Lock visualization (Canvas + touch handler) ---
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .pointerInput(sweetSpots, sweetSpotSize, totalPhases, pickDurability) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val arcR = minOf(size.width, size.height) * 0.35f
                        val touchBand = ARC_THICKNESS_DP * density * TOUCH_BAND_MULTIPLIER

                        var localPickId: Long? = null
                        var localTensionId: Long? = null

                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                for (change in event.changes) {
                                    val pos = change.position
                                    val dx = pos.x - cx
                                    val dy = pos.y - cy
                                    val dist = sqrt(dx * dx + dy * dy)

                                    if (dist < 10f) {
                                        change.consume()
                                        continue
                                    }

                                    val raw = Math.toDegrees(
                                        atan2(dy.toDouble(), dx.toDouble())
                                    ).toFloat()
                                    val cAngle = if (raw < 0) raw + 360f else raw

                                    val nearTopArc = isAngleInArc(
                                        cAngle,
                                        TOP_ARC_START - ANGLE_TOLERANCE,
                                        TOP_ARC_SWEEP + ANGLE_TOLERANCE * 2
                                    ) && abs(dist - arcR) < touchBand / 2

                                    val nearBottomArc = isAngleInArc(
                                        cAngle,
                                        BOTTOM_ARC_START - ANGLE_TOLERANCE,
                                        BOTTOM_ARC_SWEEP + ANGLE_TOLERANCE * 2
                                    ) && abs(dist - arcR) < touchBand / 2

                                    val isDown = change.pressed && !change.previousPressed
                                    val isUp = !change.pressed && change.previousPressed

                                    // --- Pointer down: assign to an arch ---
                                    if (isDown) {
                                        if (nearTopArc && localPickId == null) {
                                            localPickId = change.id.value
                                            pickAngle = normalizeAngleInArc(
                                                cAngle, TOP_ARC_START, TOP_ARC_SWEEP
                                            ).coerceIn(0f, 1f)
                                            isPickTouching = true
                                        } else if (nearBottomArc && localTensionId == null) {
                                            // Only allow tension when pick is in sweet spot
                                            val spotCenter = sweetSpots.getOrElse(localPhase) { 0.5f }
                                            val pickInSpot = isPickTouching &&
                                                abs(pickAngle - spotCenter) <= sweetSpotSize / 2
                                            if (pickInSpot) {
                                                localTensionId = change.id.value
                                                val norm = normalizeAngleInArc(
                                                    cAngle, BOTTOM_ARC_START, BOTTOM_ARC_SWEEP
                                                ).coerceIn(tensionFloor, 1f)
                                                tensionProgress = norm
                                                isTensionTouching = true
                                            }
                                        }
                                    }

                                    // --- Pick pointer tracking ---
                                    if (change.pressed && change.id.value == localPickId) {
                                        val norm = normalizeAngleInArc(
                                            cAngle, TOP_ARC_START, TOP_ARC_SWEEP
                                        ).coerceIn(0f, 1f)

                                        val spotCenter = sweetSpots.getOrElse(localPhase) { 0.5f }
                                        val wasIn = abs(pickAngle - spotCenter) <= sweetSpotSize / 2
                                        val nowIn = abs(norm - spotCenter) <= sweetSpotSize / 2

                                        // Pick left sweet spot while tension was being applied
                                        if (wasIn && !nowIn && isTensionTouching &&
                                            tensionProgress > tensionFloor + CHECKPOINT_TOLERANCE
                                        ) {
                                            onPickSlipped()
                                            tensionProgress = 0f
                                            tensionFloor = 0f
                                            isTensionTouching = false
                                            localTensionId = null
                                            localPhase = 0
                                            phaseSpotFound = false
                                        }

                                        pickAngle = norm
                                    }

                                    // --- Tension pointer tracking ---
                                    if (change.pressed && change.id.value == localTensionId) {
                                        val prog = normalizeAngleInArc(
                                            cAngle, BOTTOM_ARC_START, BOTTOM_ARC_SWEEP
                                        ).coerceIn(tensionFloor, 1f)
                                        tensionProgress = prog

                                        val spotCenter = sweetSpots.getOrElse(localPhase) { 0.5f }
                                        val inSpot = isPickTouching &&
                                            abs(pickAngle - spotCenter) <= sweetSpotSize / 2

                                        val cp = checkpoints.getOrElse(localPhase) { 1f }

                                        if (inSpot && prog >= cp - CHECKPOINT_TOLERANCE) {
                                            if (localPhase >= totalPhases - 1) {
                                                // Final phase complete — lock opened
                                                onLockPicked()
                                            } else {
                                                // Checkpoint reached — advance to next phase
                                                localPhase++
                                                tensionFloor = cp
                                                tensionProgress = cp
                                                phaseSpotFound = false
                                            }
                                        }
                                    }

                                    // --- Pointer up: release ---
                                    if (isUp) {
                                        if (change.id.value == localPickId) {
                                            localPickId = null
                                            isPickTouching = false

                                            // In multi-phase: lifting pick finger while
                                            // tension is active = slip
                                            if (totalPhases > 1 && isTensionTouching) {
                                                onPickSlipped()
                                                localPhase = 0
                                                tensionFloor = 0f
                                                phaseSpotFound = false
                                            }
                                            tensionProgress = 0f
                                            isTensionTouching = false
                                            localTensionId = null
                                        } else if (change.id.value == localTensionId) {
                                            localTensionId = null
                                            isTensionTouching = false

                                            // In multi-phase: lifting tension finger
                                            // while past first checkpoint = slip
                                            if (totalPhases > 1 && tensionFloor > 0f) {
                                                onPickSlipped()
                                                localPhase = 0
                                                tensionFloor = 0f
                                                phaseSpotFound = false
                                                isPickTouching = false
                                                localPickId = null
                                            }
                                            tensionProgress = 0f
                                        }
                                    }

                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                drawLockVisualization(
                    pickAngle = pickAngle,
                    isPickTouching = isPickTouching,
                    isInSweetSpot = isInSweetSpot,
                    tensionProgress = tensionProgress,
                    currentSweetSpot = currentSweetSpot,
                    sweetSpotSize = sweetSpotSize,
                    checkpoints = checkpoints,
                    localPhase = localPhase,
                    totalPhases = totalPhases,
                    tensionFloor = tensionFloor
                )
            }

            // --- Footer: hint + cancel ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hintText,
                    fontSize = 16.sp,
                    color = if (isInSweetSpot) Color(0xFFFFD700)
                    else Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = if (isInSweetSpot) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(12.dp))
                DecisionButton(text = "Cancel") { onCancel() }
            }
        }
    }
}

/**
 * Draws the lock visualization including arcs, keyhole, pick tool, tension wrench,
 * sweet spot glow, and checkpoint markers.
 */
private fun DrawScope.drawLockVisualization(
    pickAngle: Float,
    isPickTouching: Boolean,
    isInSweetSpot: Boolean,
    tensionProgress: Float,
    currentSweetSpot: Float,
    sweetSpotSize: Float,
    checkpoints: List<Float>,
    localPhase: Int,
    totalPhases: Int,
    tensionFloor: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arcR = minOf(size.width, size.height) * 0.35f
    val arcW = ARC_THICKNESS_DP * density
    val keyR = arcR * 0.18f
    val keyRectW = keyR * 0.9f
    val keyRectH = keyR * 1.8f
    val arcRect = Offset(cx - arcR, cy - arcR)
    val arcSize = Size(arcR * 2, arcR * 2)

    // Lock body (outer ring)
    drawCircle(
        color = Color(0xFF2A2A3E),
        radius = arcR + arcW * 0.8f,
        center = Offset(cx, cy)
    )
    // Lock body (inner)
    drawCircle(
        color = Color(0xFF1E1E30),
        radius = arcR - arcW * 0.8f,
        center = Offset(cx, cy)
    )

    // --- Top arch (blue/purple) ---
    drawArc(
        color = Color(0xFF8B9DFF).copy(alpha = 0.4f),
        startAngle = TOP_ARC_START,
        sweepAngle = TOP_ARC_SWEEP,
        useCenter = false,
        topLeft = arcRect,
        size = arcSize,
        style = Stroke(width = arcW, cap = StrokeCap.Round)
    )

    // Sweet spot proximity glow
    if (isPickTouching) {
        val nearness = 1f - (abs(pickAngle - currentSweetSpot) / 0.3f).coerceIn(0f, 1f)
        if (nearness > 0f) {
            val ssStartNorm = (currentSweetSpot - sweetSpotSize / 2).coerceIn(0f, 1f)
            val ssEndNorm = (currentSweetSpot + sweetSpotSize / 2).coerceIn(0f, 1f)
            val ssStartAngle = TOP_ARC_START + ssStartNorm * TOP_ARC_SWEEP
            val ssSweep = (ssEndNorm - ssStartNorm) * TOP_ARC_SWEEP
            drawArc(
                color = Color(0xFFFFD700).copy(alpha = nearness * 0.6f),
                startAngle = ssStartAngle,
                sweepAngle = ssSweep,
                useCenter = false,
                topLeft = arcRect,
                size = arcSize,
                style = Stroke(width = arcW + 8f, cap = StrokeCap.Round)
            )
        }
    }

    // --- Bottom arch (red/coral) ---
    val bottomAlpha = if (isInSweetSpot) 0.6f else 0.25f
    drawArc(
        color = Color(0xFFFF8A80).copy(alpha = bottomAlpha),
        startAngle = BOTTOM_ARC_START,
        sweepAngle = BOTTOM_ARC_SWEEP,
        useCenter = false,
        topLeft = arcRect,
        size = arcSize,
        style = Stroke(width = arcW, cap = StrokeCap.Round)
    )

    // Bottom arch progress fill
    if (tensionProgress > 0.01f) {
        drawArc(
            color = Color(0xFFFF5252),
            startAngle = BOTTOM_ARC_START,
            sweepAngle = BOTTOM_ARC_SWEEP * tensionProgress,
            useCenter = false,
            topLeft = arcRect,
            size = arcSize,
            style = Stroke(width = arcW + 4f, cap = StrokeCap.Round)
        )
    }

    // --- Checkpoint markers on bottom arch ---
    if (totalPhases > 1) {
        checkpoints.forEachIndexed { i, cp ->
            if (i < totalPhases - 1) { // Don't draw marker at 100% (the end)
                val cpAngle = BOTTOM_ARC_START + cp * BOTTOM_ARC_SWEEP
                val cpRad = Math.toRadians(cpAngle.toDouble())
                val markerColor = when {
                    i < localPhase -> Color.Green  // Already passed
                    i == localPhase -> Color.Yellow // Current target
                    else -> Color.White.copy(alpha = 0.5f)
                }

                // Draw checkpoint tick mark
                val innerR = arcR - arcW * 0.7f
                val outerR = arcR + arcW * 0.7f
                drawLine(
                    color = markerColor,
                    start = Offset(
                        cx + innerR * cos(cpRad).toFloat(),
                        cy + innerR * sin(cpRad).toFloat()
                    ),
                    end = Offset(
                        cx + outerR * cos(cpRad).toFloat(),
                        cy + outerR * sin(cpRad).toFloat()
                    ),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Draw small circle at checkpoint
                drawCircle(
                    color = markerColor,
                    radius = 6f,
                    center = Offset(
                        cx + arcR * cos(cpRad).toFloat(),
                        cy + arcR * sin(cpRad).toFloat()
                    )
                )
            }
        }
    }

    // --- Keyhole ---
    drawCircle(
        color = Color(0xFF0D0D1A),
        radius = keyR,
        center = Offset(cx, cy - keyRectH * 0.15f)
    )
    drawRect(
        color = Color(0xFF0D0D1A),
        topLeft = Offset(cx - keyRectW / 2, cy - keyRectH * 0.15f),
        size = Size(keyRectW, keyRectH)
    )

    // --- Pick tool ---
    val pickCanvasAngle = TOP_ARC_START + pickAngle * TOP_ARC_SWEEP
    val pickRad = Math.toRadians(pickCanvasAngle.toDouble())
    val pickColor = if (isInSweetSpot) Color(0xFFFFD700) else Color(0xFFBBBBBB)
    val pickStartR = keyR * 1.5f
    val pickEndR = arcR

    val pickStart = Offset(
        cx + pickStartR * cos(pickRad).toFloat(),
        cy + pickStartR * sin(pickRad).toFloat()
    )
    val pickEnd = Offset(
        cx + pickEndR * cos(pickRad).toFloat(),
        cy + pickEndR * sin(pickRad).toFloat()
    )

    drawLine(
        color = pickColor,
        start = pickStart,
        end = pickEnd,
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = pickColor,
        radius = 8f,
        center = pickEnd
    )
    if (isInSweetSpot) {
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.3f),
            radius = 22f,
            center = pickEnd
        )
    }

    // --- Tension wrench ---
    val wrenchBaseAngle = 90.0 // points straight down
    val wrenchRotation = tensionProgress.toDouble() * 25.0
    val wrenchRad = Math.toRadians(wrenchBaseAngle + wrenchRotation)
    val wrStartR = keyR * 0.5f
    val wrEndR = arcR * 1.1f

    val wrenchStart = Offset(
        cx + wrStartR * cos(wrenchRad).toFloat(),
        cy + wrStartR * sin(wrenchRad).toFloat()
    )
    val wrenchEnd = Offset(
        cx + wrEndR * cos(wrenchRad).toFloat(),
        cy + wrEndR * sin(wrenchRad).toFloat()
    )

    drawLine(
        color = Color(0xFF888888),
        start = wrenchStart,
        end = wrenchEnd,
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )

    // Wrench handle
    val perpRad = wrenchRad + Math.PI / 2
    val handleHalf = 14f
    drawLine(
        color = Color(0xFF666666),
        start = Offset(
            wrenchEnd.x - handleHalf * cos(perpRad).toFloat(),
            wrenchEnd.y - handleHalf * sin(perpRad).toFloat()
        ),
        end = Offset(
            wrenchEnd.x + handleHalf * cos(perpRad).toFloat(),
            wrenchEnd.y + handleHalf * sin(perpRad).toFloat()
        ),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
}

/** Checks if a canvas angle falls within an arc defined by start angle + sweep. */
private fun isAngleInArc(angle: Float, arcStart: Float, arcSweep: Float): Boolean {
    val arcEnd = arcStart + arcSweep
    return if (arcEnd <= 360f) {
        angle in arcStart..arcEnd
    } else {
        angle >= arcStart || angle <= (arcEnd - 360f)
    }
}

/** Normalizes a canvas angle to a 0-1 value within an arc, handling wrap-around. */
private fun normalizeAngleInArc(angle: Float, arcStart: Float, arcSweep: Float): Float {
    var offset = angle - arcStart
    if (offset < -180f) offset += 360f
    if (offset > 180f) offset -= 360f
    return offset / arcSweep
}
