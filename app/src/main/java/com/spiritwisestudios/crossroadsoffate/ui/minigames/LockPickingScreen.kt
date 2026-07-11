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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameState
import com.spiritwisestudios.crossroadsoffate.minigames.games.LockPickingData
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

private object LockPickingColors {
    val ScreenBackground = Color(0xFF1A1A2E)
    val TimerBarBackground = Color(0xFF333333)
    val InactivePhaseIndicator = Color(0xFF555555)
    val AccentBlue = Color(0xFF8B9DFF)
    val SweetSpotGold = Color(0xFFFFD700)
    val LockBodyOuter = Color(0xFF2A2A3E)
    val LockBodyInner = Color(0xFF1E1E30)
    val BottomArchCoral = Color(0xFFFF8A80)
    val BottomArchFillRed = Color(0xFFFF5252)
    val KeyholeDark = Color(0xFF0D0D1A)
    val PickSilver = Color(0xFFBBBBBB)
    val WrenchGray = Color(0xFF888888)
    val WrenchStrainRed = Color(0xFFFF4444)
    val WrenchHandleGray = Color(0xFF666666)
    val WrenchHandleStrainRed = Color(0xFFCC2222)
}

private data class LockPickState(
    val pickAngle: Float,
    val isPickTouching: Boolean,
    val isInSweetSpot: Boolean,
    val currentSweetSpot: Float,
    val sweetSpotSize: Float
)

private data class TensionState(
    val tensionProgress: Float,
    val tensionFingerRaw: Float,
    val tensionFloor: Float,
    val wrenchStrainRatio: Float
)

private data class PhaseState(
    val checkpoints: List<Float>,
    val localPhase: Int,
    val totalPhases: Int
)

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
    val lockData = gameState.data as? LockPickingData ?: return
    val sweetSpots = lockData.sweetSpots
    val checkpoints = lockData.checkpoints
    val sweetSpotSize = lockData.sweetSpotSize
    val totalPhases = lockData.totalPhases
    val pickDurability = lockData.pickDurability
    val startTime = lockData.startTimeMillis
    val timeLimit = gameState.timeRemaining ?: 60
    val lastResult = lockData.lastResult

    // --- Local interaction state ---
    // Current phase within this attempt (0-indexed, advances locally until all done)
    var localPhase by remember { mutableIntStateOf(0) }
    var pickAngle by remember { mutableFloatStateOf(0.5f) }
    var isPickTouching by remember { mutableStateOf(false) }
    var tensionProgress by remember { mutableFloatStateOf(0f) }
    // Raw finger position on bottom arch (unclamped, for wrench tip drawing)
    var tensionFingerRaw by remember { mutableFloatStateOf(0f) }
    var isTensionTouching by remember { mutableStateOf(false) }
    // Tracks the bottom arch floor — after reaching a checkpoint, tension can't go below it
    var tensionFloor by remember { mutableFloatStateOf(0f) }
    // Whether the current phase's sweet spot has been found (pick is in it)
    var phaseSpotFound by remember { mutableStateOf(false) }
    // Incremented each time a checkpoint is reached, drives haptic feedback
    var checkpointHitCount by remember { mutableIntStateOf(0) }
    // Wrench strain from bending — accumulates while finger is past checkpoint
    var wrenchStrain by remember { mutableFloatStateOf(0f) }

    val currentSweetSpot = sweetSpots.getOrElse(localPhase) { 0.5f }
    val currentCheckpoint = checkpoints.getOrElse(localPhase) { 1f }

    val isInSweetSpot = isPickTouching &&
        LockPickingGame.isAngleInSweetSpot(pickAngle, currentSweetSpot, sweetSpotSize)

    // Reset all local state when game state changes (new attempt after slip, or game restart)
    LaunchedEffect(sweetSpots, pickDurability) {
        localPhase = 0
        pickAngle = 0.5f
        tensionProgress = 0f
        tensionFingerRaw = 0f
        tensionFloor = 0f
        isPickTouching = false
        isTensionTouching = false
        phaseSpotFound = false
        checkpointHitCount = 0
        wrenchStrain = 0f
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

    // Haptic feedback when a checkpoint is reached
    LaunchedEffect(checkpointHitCount) {
        if (checkpointHitCount > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    val breakThreshold = LockPickingGame.strainBreakThreshold(pickDurability)

    // Wrench strain: bending past checkpoint accumulates damage, eventually breaks.
    // This LaunchedEffect(Unit) reads mutable state vars (isTensionTouching, tensionFingerRaw, etc.)
    // through closures — Compose snapshot state ensures reads always see the latest values without
    // needing to re-launch the effect.
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            wrenchStrain = LockPickingGame.nextWrenchStrain(
                current = wrenchStrain,
                tensionFingerRaw = tensionFingerRaw,
                tensionProgress = tensionProgress,
                isTensionTouching = isTensionTouching
            )
            if (wrenchStrain >= breakThreshold) {
                wrenchStrain = 0f
                onPickSlipped()
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
    // Updates once per second since the display only shows whole seconds.
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
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
        isInSweetSpot && isTensionTouching && tensionProgress < currentCheckpoint - LockPickingGame.CHECKPOINT_TOLERANCE ->
            "Keep swiping to the checkpoint..."
        isInSweetSpot && localPhase < totalPhases - 1 ->
            "Hold bottom! Slide top to next position"
        isInSweetSpot && isTensionTouching -> "Almost there... complete the swipe!"
        else -> "Don't let go!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LockPickingColors.ScreenBackground)
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
                        .background(LockPickingColors.TimerBarBackground, RoundedCornerShape(7.dp))
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
                                                else -> LockPickingColors.InactivePhaseIndicator
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
                                        if (i < pickDurability) LockPickingColors.AccentBlue
                                        else Color.Red,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            // --- Lock visualization (Canvas + touch handler) ---
            // The pointerInput and DrawScope each compute cx/cy/arcR independently because
            // Compose pointerInput and DrawScope have separate `size` access — pointerInput
            // uses PointerInputScope.size while DrawScope uses DrawScope.size. Both resolve
            // to the same layout size, but the values cannot be shared across scopes.
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

                        val resetPhaseOnSlip = {
                            onPickSlipped()
                            localPhase = 0
                            tensionFloor = 0f
                            phaseSpotFound = false
                        }

                        val resetTensionTracking = {
                            tensionProgress = 0f
                            tensionFingerRaw = 0f
                            isTensionTouching = false
                            localTensionId = null
                        }

                        fun handlePointerDown(
                            change: PointerInputChange,
                            cAngle: Float,
                            nearTopArc: Boolean,
                            nearBottomArc: Boolean
                        ) {
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
                                    LockPickingGame.isAngleInSweetSpot(pickAngle, spotCenter, sweetSpotSize)
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

                        fun handlePickTracking(cAngle: Float) {
                            val norm = normalizeAngleInArc(
                                cAngle, TOP_ARC_START, TOP_ARC_SWEEP
                            ).coerceIn(0f, 1f)

                            val spotCenter = sweetSpots.getOrElse(localPhase) { 0.5f }
                            val wasIn = LockPickingGame.isAngleInSweetSpot(pickAngle, spotCenter, sweetSpotSize)
                            val nowIn = LockPickingGame.isAngleInSweetSpot(norm, spotCenter, sweetSpotSize)

                            // Pick left sweet spot while tension was being applied
                            if (wasIn && !nowIn && isTensionTouching &&
                                tensionProgress > tensionFloor + LockPickingGame.CHECKPOINT_TOLERANCE
                            ) {
                                resetPhaseOnSlip()
                                resetTensionTracking()
                            }

                            pickAngle = norm
                        }

                        fun handleTensionTracking(cAngle: Float) {
                            val spotCenter = sweetSpots.getOrElse(localPhase) { 0.5f }
                            val inSpot = isPickTouching &&
                                LockPickingGame.isAngleInSweetSpot(pickAngle, spotCenter, sweetSpotSize)

                            if (inSpot && !phaseSpotFound) {
                                phaseSpotFound = true
                            }

                            // Cap tension at the floor (checkpoint) until
                            // the pick finds the new sweet spot
                            val cp = checkpoints.getOrElse(localPhase) { 1f }
                            val upperBound = if (phaseSpotFound) cp else tensionFloor
                            val rawProg = normalizeAngleInArc(
                                cAngle, BOTTOM_ARC_START, BOTTOM_ARC_SWEEP
                            ).coerceIn(tensionFloor, 1f)
                            tensionFingerRaw = rawProg
                            val prog = rawProg.coerceAtMost(upperBound)
                            tensionProgress = prog

                            if (inSpot && prog >= cp - LockPickingGame.CHECKPOINT_TOLERANCE) {
                                if (localPhase >= totalPhases - 1) {
                                    // Final phase complete — lock opened
                                    onLockPicked()
                                } else {
                                    // Checkpoint reached — advance to next phase
                                    localPhase++
                                    tensionFloor = cp
                                    tensionProgress = cp
                                    phaseSpotFound = false
                                    checkpointHitCount++
                                }
                            }
                        }

                        fun handlePointerUp(pointerId: Long) {
                            if (pointerId == localPickId) {
                                localPickId = null
                                isPickTouching = false

                                // In multi-phase: lifting pick finger while
                                // tension is active = slip
                                if (totalPhases > 1 && isTensionTouching) {
                                    resetPhaseOnSlip()
                                }
                                resetTensionTracking()
                            } else if (pointerId == localTensionId) {
                                localTensionId = null
                                isTensionTouching = false

                                // In multi-phase: lifting tension finger
                                // while past first checkpoint = slip
                                if (totalPhases > 1 && tensionFloor > 0f) {
                                    resetPhaseOnSlip()
                                    isPickTouching = false
                                    localPickId = null
                                }
                                tensionProgress = 0f
                                tensionFingerRaw = 0f
                            }
                        }

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

                                    if (isDown) handlePointerDown(change, cAngle, nearTopArc, nearBottomArc)
                                    if (change.pressed && change.id.value == localPickId) handlePickTracking(cAngle)
                                    if (change.pressed && change.id.value == localTensionId) handleTensionTracking(cAngle)
                                    if (isUp) handlePointerUp(change.id.value)

                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                val strainRatio = if (breakThreshold > 0f)
                    (wrenchStrain / breakThreshold).coerceIn(0f, 1f) else 0f
                drawLockVisualization(
                    pick = LockPickState(
                        pickAngle = pickAngle,
                        isPickTouching = isPickTouching,
                        isInSweetSpot = isInSweetSpot,
                        currentSweetSpot = currentSweetSpot,
                        sweetSpotSize = sweetSpotSize
                    ),
                    tension = TensionState(
                        tensionProgress = tensionProgress,
                        tensionFingerRaw = tensionFingerRaw,
                        tensionFloor = tensionFloor,
                        wrenchStrainRatio = strainRatio
                    ),
                    phase = PhaseState(
                        checkpoints = checkpoints,
                        localPhase = localPhase,
                        totalPhases = totalPhases
                    )
                )
            }

            // --- Footer: hint + cancel ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hintText,
                    fontSize = 16.sp,
                    color = if (isInSweetSpot) LockPickingColors.SweetSpotGold
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
 *
 * cx/cy/arcR are computed here independently from the pointerInput scope because
 * Compose DrawScope and PointerInputScope have separate `size` properties. Both
 * resolve to the same layout size but cannot share values across scopes.
 */
private fun DrawScope.drawLockVisualization(
    pick: LockPickState,
    tension: TensionState,
    phase: PhaseState
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arcR = minOf(size.width, size.height) * 0.35f
    val arcW = ARC_THICKNESS_DP * density
    val keyR = arcR * 0.18f
    val arcRect = Offset(cx - arcR, cy - arcR)
    val arcSize = Size(arcR * 2, arcR * 2)

    drawLockBody(cx, cy, arcR, arcW)
    drawTopArch(pick, cx, cy, arcR, arcW, arcRect, arcSize)
    drawBottomArch(pick, tension, phase, arcW, arcRect, arcSize)
    drawCheckpointMarkers(phase, cx, cy, arcR, arcW)
    drawKeyhole(cx, cy, keyR)
    drawPickTool(pick, cx, cy, keyR, arcR)
    drawTensionWrench(tension, cx, cy, keyR, arcR)
}

private fun DrawScope.drawLockBody(cx: Float, cy: Float, arcR: Float, arcW: Float) {
    drawCircle(
        color = LockPickingColors.LockBodyOuter,
        radius = arcR + arcW * 0.8f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = LockPickingColors.LockBodyInner,
        radius = arcR - arcW * 0.8f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawTopArch(
    pick: LockPickState,
    cx: Float, cy: Float, arcR: Float, arcW: Float,
    arcRect: Offset, arcSize: Size
) {
    drawArc(
        color = LockPickingColors.AccentBlue.copy(alpha = 0.4f),
        startAngle = TOP_ARC_START,
        sweepAngle = TOP_ARC_SWEEP,
        useCenter = false,
        topLeft = arcRect,
        size = arcSize,
        style = Stroke(width = arcW, cap = StrokeCap.Round)
    )

    // Sweet spot proximity glow
    if (pick.isPickTouching) {
        val nearness = 1f - (abs(pick.pickAngle - pick.currentSweetSpot) / 0.3f).coerceIn(0f, 1f)
        if (nearness > 0f) {
            val ssStartNorm = (pick.currentSweetSpot - pick.sweetSpotSize / 2).coerceIn(0f, 1f)
            val ssEndNorm = (pick.currentSweetSpot + pick.sweetSpotSize / 2).coerceIn(0f, 1f)
            val ssStartAngle = TOP_ARC_START + ssStartNorm * TOP_ARC_SWEEP
            val ssSweep = (ssEndNorm - ssStartNorm) * TOP_ARC_SWEEP
            drawArc(
                color = LockPickingColors.SweetSpotGold.copy(alpha = nearness * 0.6f),
                startAngle = ssStartAngle,
                sweepAngle = ssSweep,
                useCenter = false,
                topLeft = arcRect,
                size = arcSize,
                style = Stroke(width = arcW + 8f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawBottomArch(
    pick: LockPickState,
    tension: TensionState,
    phase: PhaseState,
    arcW: Float, arcRect: Offset, arcSize: Size
) {
    val bottomAlpha = if (pick.isInSweetSpot) 0.6f else 0.25f
    drawArc(
        color = LockPickingColors.BottomArchCoral.copy(alpha = bottomAlpha),
        startAngle = BOTTOM_ARC_START,
        sweepAngle = BOTTOM_ARC_SWEEP,
        useCenter = false,
        topLeft = arcRect,
        size = arcSize,
        style = Stroke(width = arcW, cap = StrokeCap.Round)
    )

    // Progress fill
    if (tension.tensionProgress > 0.01f) {
        val fillSweep = BOTTOM_ARC_SWEEP * tension.tensionProgress
        val atCheckpoint = phase.totalPhases > 1 && phase.checkpoints.any { cp ->
            cp < 1f && abs(tension.tensionProgress - cp) < LockPickingGame.CHECKPOINT_TOLERANCE
        }
        if (atCheckpoint) {
            // Rounded cap at the start, flat cap at the checkpoint for a clean stop
            val capSweep = minOf(2f, fillSweep)
            drawBottomFillArc(capSweep, arcW, arcRect, arcSize, StrokeCap.Round)
            drawBottomFillArc(fillSweep, arcW, arcRect, arcSize, StrokeCap.Butt)
        } else {
            drawBottomFillArc(fillSweep, arcW, arcRect, arcSize, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawBottomFillArc(
    sweep: Float, arcW: Float,
    arcRect: Offset, arcSize: Size, cap: StrokeCap
) {
    drawArc(
        color = LockPickingColors.BottomArchFillRed,
        startAngle = BOTTOM_ARC_START,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = arcRect,
        size = arcSize,
        style = Stroke(width = arcW + 4f, cap = cap)
    )
}

private fun DrawScope.drawCheckpointMarkers(
    phase: PhaseState,
    cx: Float, cy: Float, arcR: Float, arcW: Float
) {
    if (phase.totalPhases <= 1) return

    phase.checkpoints.forEachIndexed { i, cp ->
        if (i < phase.totalPhases - 1) { // Don't draw marker at 100% (the end)
            val cpAngle = BOTTOM_ARC_START + cp * BOTTOM_ARC_SWEEP
            val cpRad = Math.toRadians(cpAngle.toDouble())
            val markerColor = when {
                i < phase.localPhase -> Color.Green
                i == phase.localPhase -> Color.Yellow
                else -> Color.White.copy(alpha = 0.5f)
            }

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

private fun DrawScope.drawKeyhole(cx: Float, cy: Float, keyR: Float) {
    val keyRectW = keyR * 0.9f
    val keyRectH = keyR * 1.8f
    drawCircle(
        color = LockPickingColors.KeyholeDark,
        radius = keyR,
        center = Offset(cx, cy - keyRectH * 0.15f)
    )
    drawRect(
        color = LockPickingColors.KeyholeDark,
        topLeft = Offset(cx - keyRectW / 2, cy - keyRectH * 0.15f),
        size = Size(keyRectW, keyRectH)
    )
}

private fun DrawScope.drawPickTool(
    pick: LockPickState,
    cx: Float, cy: Float, keyR: Float, arcR: Float
) {
    val pickCanvasAngle = TOP_ARC_START + pick.pickAngle * TOP_ARC_SWEEP
    val pickRad = Math.toRadians(pickCanvasAngle.toDouble())
    val pickColor = if (pick.isInSweetSpot) LockPickingColors.SweetSpotGold else LockPickingColors.PickSilver
    val pickStartR = keyR * 1.5f

    val pickStart = Offset(
        cx + pickStartR * cos(pickRad).toFloat(),
        cy + pickStartR * sin(pickRad).toFloat()
    )
    val pickEnd = Offset(
        cx + arcR * cos(pickRad).toFloat(),
        cy + arcR * sin(pickRad).toFloat()
    )

    drawLine(
        color = pickColor,
        start = pickStart,
        end = pickEnd,
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    drawCircle(color = pickColor, radius = 8f, center = pickEnd)
    if (pick.isInSweetSpot) {
        drawCircle(
            color = LockPickingColors.SweetSpotGold.copy(alpha = 0.3f),
            radius = 22f,
            center = pickEnd
        )
    }
}

private fun DrawScope.drawTensionWrench(
    tension: TensionState,
    cx: Float, cy: Float, keyR: Float, arcR: Float
) {
    val wrenchColor = lerp(LockPickingColors.WrenchGray, LockPickingColors.WrenchStrainRed, tension.wrenchStrainRatio)
    val wrenchHandleColor = lerp(LockPickingColors.WrenchHandleGray, LockPickingColors.WrenchHandleStrainRed, tension.wrenchStrainRatio)
    val wrStartR = keyR * 0.5f
    val wrEndR = arcR * 1.1f

    val clampedAngle = BOTTOM_ARC_START + tension.tensionProgress * BOTTOM_ARC_SWEEP
    val clampedRad = Math.toRadians(clampedAngle.toDouble())
    val fingerAngle = BOTTOM_ARC_START + tension.tensionFingerRaw * BOTTOM_ARC_SWEEP
    val fingerRad = Math.toRadians(fingerAngle.toDouble())

    val fingerPastCheckpoint = tension.tensionFingerRaw > tension.tensionProgress + LockPickingGame.CHECKPOINT_TOLERANCE

    val wrenchStart = Offset(
        cx + wrStartR * cos(clampedRad).toFloat(),
        cy + wrStartR * sin(clampedRad).toFloat()
    )

    if (fingerPastCheckpoint) {
        val wrenchTip = Offset(
            cx + wrEndR * cos(fingerRad).toFloat(),
            cy + wrEndR * sin(fingerRad).toFloat()
        )
        // Cubic Bezier: bend starts near the lock, outer portion runs straight to finger
        val cp1R = wrStartR + (wrEndR - wrStartR) * 0.33f
        val cp1 = Offset(
            cx + cp1R * cos(clampedRad).toFloat(),
            cy + cp1R * sin(clampedRad).toFloat()
        )
        val cp2R = wrStartR + (wrEndR - wrStartR) * 0.67f
        val cp2 = Offset(
            cx + cp2R * cos(fingerRad).toFloat(),
            cy + cp2R * sin(fingerRad).toFloat()
        )

        val wrenchPath = Path().apply {
            moveTo(wrenchStart.x, wrenchStart.y)
            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, wrenchTip.x, wrenchTip.y)
        }
        drawPath(
            path = wrenchPath,
            color = wrenchColor,
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        drawWrenchHandle(wrenchTip, fingerRad, wrenchHandleColor)
    } else {
        val wrenchEnd = Offset(
            cx + wrEndR * cos(clampedRad).toFloat(),
            cy + wrEndR * sin(clampedRad).toFloat()
        )
        drawLine(
            color = wrenchColor,
            start = wrenchStart,
            end = wrenchEnd,
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
        drawWrenchHandle(wrenchEnd, clampedRad, wrenchHandleColor)
    }
}

private fun DrawScope.drawWrenchHandle(tip: Offset, angleRad: Double, color: Color) {
    val perpRad = angleRad + Math.PI / 2
    val handleHalf = 14f
    drawLine(
        color = color,
        start = Offset(
            tip.x - handleHalf * cos(perpRad).toFloat(),
            tip.y - handleHalf * sin(perpRad).toFloat()
        ),
        end = Offset(
            tip.x + handleHalf * cos(perpRad).toFloat(),
            tip.y + handleHalf * sin(perpRad).toFloat()
        ),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
}

/** Checks if a normalized angle (0-1) falls within the sweet spot centered at [center] with the given [size]. */
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
