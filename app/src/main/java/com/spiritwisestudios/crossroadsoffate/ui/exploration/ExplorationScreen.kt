package com.spiritwisestudios.crossroadsoffate.ui.exploration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMap
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntity
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntityType
import com.spiritwisestudios.crossroadsoffate.logic.ExplorationPlayerState
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.ui.components.GameCard
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Avatar palette (fixed for now; swap for sprites later without touching the manager)
private val AvatarCloak = Color(0xFF3E6B8A)
private val AvatarTrim = Color(0xFFD9A441)
private val AvatarSkin = Color(0xFFF2C9A0)
private val AvatarHair = Color(0xFF5D4230)
private val AvatarLegs = Color(0xFF2C3E50)

/**
 * Free-roam exploration between story beats: the avatar walks around a themed map,
 * talks to ambient NPCs, follows exits to neighboring maps, and reaches the pulsing
 * story marker to continue the narrative. Tap anywhere to walk; tap an icon to use it.
 */
@Composable
fun ExplorationScreen(gameViewModel: GameViewModel) {
    val map by gameViewModel.explorationMap.collectAsState()
    val player by gameViewModel.explorationPlayer.collectAsState()
    val dialog by gameViewModel.explorationDialog.collectAsState()
    val showStoryMarker by gameViewModel.isStoryMarkerVisible.collectAsState()
    val completedActivities by gameViewModel.completedActivities.collectAsState()

    val currentMap = map ?: return

    // Drive movement from the frame clock so walk speed is time-based, not frame-based
    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrame != 0L) {
                    gameViewModel.updateExploration((now - lastFrame) / 1_000_000)
                }
                lastFrame = now
            }
        }
    }

    val pulse by rememberInfiniteTransition(label = "storyPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "storyPulseValue"
    )

    val groundColor = parseMapColor(currentMap.theme.ground, Color(0xFF6B8F5A))
    val detailColor = parseMapColor(currentMap.theme.groundDetail, Color(0xFF5C7C4D))
    val obstacleFill = parseMapColor(currentMap.theme.obstacleFill, Color(0xFF5B4A3A))
    val obstacleStroke = parseMapColor(currentMap.theme.obstacleStroke, Color(0xFF3E3228))
    val accentColor = parseMapColor(currentMap.theme.accent, GameColors.Gold)

    // Deterministic scatter of ground detail (grass tufts / pebbles) per map
    val groundDetails = remember(currentMap.id) {
        val random = Random(currentMap.id.hashCode())
        List((currentMap.width * currentMap.height / 5000f).toInt().coerceIn(40, 220)) {
            Triple(
                random.nextFloat() * currentMap.width,
                random.nextFloat() * currentMap.height,
                1.2f + random.nextFloat() * 2.2f
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(darken(groundColor, 0.45f))
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val scale = min(screenW / currentMap.width, screenH / currentMap.height)
        val offsetX = (screenW - currentMap.width * scale) / 2f
        val offsetY = (screenH - currentMap.height * scale) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("explorationCanvas")
                .pointerInput(currentMap.id, scale, offsetX, offsetY) {
                    detectTapGestures { tap ->
                        gameViewModel.onExplorationTap(
                            (tap.x - offsetX) / scale,
                            (tap.y - offsetY) / scale
                        )
                    }
                }
        ) {
            val t = WorldTransform(scale, offsetX, offsetY)

            // Ground
            drawRect(
                color = groundColor,
                topLeft = Offset(t.x(0f), t.y(0f)),
                size = Size(t.s(currentMap.width), t.s(currentMap.height))
            )
            groundDetails.forEach { (dx, dy, r) ->
                drawCircle(detailColor.copy(alpha = 0.55f), t.s(r), Offset(t.x(dx), t.y(dy)))
            }

            // Obstacles (buildings, rocks, water...)
            currentMap.obstacles.forEach { obstacle ->
                val corner = t.s(min(10f, min(obstacle.width, obstacle.height) / 4f))
                drawRoundRect(
                    color = obstacleFill,
                    topLeft = Offset(t.x(obstacle.x), t.y(obstacle.y)),
                    size = Size(t.s(obstacle.width), t.s(obstacle.height)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner)
                )
                drawRoundRect(
                    color = obstacleStroke,
                    topLeft = Offset(t.x(obstacle.x), t.y(obstacle.y)),
                    size = Size(t.s(obstacle.width), t.s(obstacle.height)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner),
                    style = Stroke(width = t.s(2f))
                )
                obstacle.icon?.let { icon ->
                    drawEmoji(
                        icon,
                        t.x(obstacle.x + obstacle.width / 2f),
                        t.y(obstacle.y + obstacle.height / 2f),
                        t.s(min(obstacle.width, obstacle.height) * 0.55f)
                    )
                }
                obstacle.label?.let { label ->
                    drawMapLabel(
                        label,
                        t.x(obstacle.x + obstacle.width / 2f),
                        t.y(obstacle.y + obstacle.height) - t.s(4f),
                        t.s(10f)
                    )
                }
            }

            // Decorations (non-colliding scenery)
            currentMap.decor.forEach { decor ->
                drawEmoji(decor.icon, t.x(decor.x), t.y(decor.y), t.s(24f * decor.scale))
            }

            // Interactive entities
            currentMap.entities.forEach { entity ->
                if (entity.type == MapEntityType.STORY && !showStoryMarker) return@forEach
                drawEntity(entity, t, player, pulse, accentColor, completedActivities)
            }

            // The player avatar, drawn last so it stays in front
            drawPlayerAvatar(player, t)

            // Map frame
            drawRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(t.x(0f), t.y(0f)),
                size = Size(t.s(currentMap.width), t.s(currentMap.height)),
                style = Stroke(width = t.s(3f))
            )
        }

        // --- HUD ---

        // Location name + story hint
        GameCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            contentPadding = 8.dp
        ) {
            Text(
                text = currentMap.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            if (showStoryMarker) {
                Text(
                    text = "Walk to ❗ to continue the story",
                    fontSize = 11.sp,
                    color = GameColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Skip straight to the pending scenario
        if (showStoryMarker) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                DecisionButton(
                    text = "Skip ➤",
                    modifier = Modifier
                        .testTag("skipExplorationButton")
                        .semantics { contentDescription = "Skip exploration" }
                ) {
                    gameViewModel.playSfx("button_tap")
                    gameViewModel.skipExploration()
                }
            }
        }

        // NPC dialog bubble
        dialog?.let { npcDialog ->
            GameCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                    .widthIn(max = 420.dp)
                    .testTag("explorationDialog")
                    .clickable { gameViewModel.advanceExplorationDialog() },
                backgroundColor = GameColors.DialogBackground,
                contentPadding = 12.dp
            ) {
                Row {
                    Text(text = npcDialog.icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = npcDialog.speakerName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GameColors.Gold
                        )
                        Text(
                            text = npcDialog.line,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        if (npcDialog.totalLines > 1) {
                            Text(
                                text = "tap for more · ${npcDialog.lineNumber}/${npcDialog.totalLines}",
                                fontSize = 11.sp,
                                color = GameColors.TextSecondary
                            )
                        }
                    }
                    Text(
                        text = "✕",
                        fontSize = 16.sp,
                        color = GameColors.TextSecondary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { gameViewModel.dismissExplorationDialog() }
                            .semantics { contentDescription = "Dismiss dialog" }
                    )
                }
            }
        }
    }
}

/** World-unit → screen-pixel transform for the letterboxed, aspect-fit map. */
private class WorldTransform(val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun x(worldX: Float): Float = offsetX + worldX * scale
    fun y(worldY: Float): Float = offsetY + worldY * scale
    fun s(worldSize: Float): Float = worldSize * scale
}

private fun DrawScope.drawEntity(
    entity: MapEntity,
    t: WorldTransform,
    player: ExplorationPlayerState,
    pulse: Float,
    accent: Color,
    completedActivities: Set<String>
) {
    val cx = t.x(entity.x)
    val cy = t.y(entity.y)
    val isStory = entity.type == MapEntityType.STORY
    val isActivity = entity.type == MapEntityType.ACTIVITY

    // Proximity ring when the avatar is close enough to interact soon
    val dx = entity.x - player.position.x
    val dy = entity.y - player.position.y
    if (dx * dx + dy * dy < 70f * 70f) {
        drawCircle(Color.White.copy(alpha = 0.30f), t.s(24f), Offset(cx, cy), style = Stroke(t.s(2f)))
    }

    if (isStory) {
        // Expanding gold pulse so the story beat is unmissable
        drawCircle(
            GameColors.Gold.copy(alpha = (1f - pulse) * 0.8f),
            t.s(20f + pulse * 16f),
            Offset(cx, cy),
            style = Stroke(t.s(3f))
        )
        drawCircle(GameColors.Gold.copy(alpha = 0.9f), t.s(19f), Offset(cx, cy), style = Stroke(t.s(2f)))
    }

    if (isActivity) {
        // Steady accent ring marks something playable here
        drawCircle(accent.copy(alpha = 0.85f), t.s(20f), Offset(cx, cy), style = Stroke(t.s(2f)))
    }

    // Icon badge
    val badgeColor = if (isStory) Color(0xB3403010) else Color.Black.copy(alpha = 0.45f)
    drawCircle(badgeColor, t.s(17f), Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.75f), t.s(17f), Offset(cx, cy), style = Stroke(t.s(1.5f)))

    val bounce = if (isStory) sin(pulse * 2f * Math.PI).toFloat() * t.s(3f) else 0f
    drawEmoji(entity.icon, cx, cy + bounce, t.s(22f))

    // Green tick once the linked activity has been completed
    if (isActivity && entity.activityId != null && completedActivities.contains(entity.activityId)) {
        val tickCenter = Offset(cx + t.s(13f), cy - t.s(13f))
        drawCircle(Color(0xFF2E8B57), t.s(7f), tickCenter)
        drawCircle(Color.White.copy(alpha = 0.9f), t.s(7f), tickCenter, style = Stroke(t.s(1f)))
        drawMapLabel("✓", tickCenter.x, tickCenter.y + t.s(3.5f), t.s(10f))
    }

    if (entity.label.isNotEmpty()) {
        drawMapLabel(entity.label, cx, cy + t.s(30f), t.s(11f))
    }
}

/**
 * The placeholder hero: a hooded wanderer built from primitives with a walk bob,
 * swinging legs, and an eye dot that shows facing. Replace with sprites later.
 */
private fun DrawScope.drawPlayerAvatar(player: ExplorationPlayerState, t: WorldTransform) {
    val x = player.position.x
    val y = player.position.y
    val facing = if (player.facingLeft) -1f else 1f
    val bob = if (player.isMoving) sin(player.distanceTraveled * 0.3f) * 1.6f else 0f
    val legSwing = if (player.isMoving) sin(player.distanceTraveled * 0.3f) * 4.5f else 0f

    // Drop shadow at the feet
    drawOval(
        Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(t.x(x - 10f), t.y(y - 3f)),
        size = Size(t.s(20f), t.s(6f))
    )

    // Legs (swing while walking)
    drawLine(
        AvatarLegs,
        Offset(t.x(x - 3f), t.y(y - 10f + bob)),
        Offset(t.x(x - 3f + legSwing * facing), t.y(y)),
        strokeWidth = t.s(4f)
    )
    drawLine(
        AvatarLegs,
        Offset(t.x(x + 3f), t.y(y - 10f + bob)),
        Offset(t.x(x + 3f - legSwing * facing), t.y(y)),
        strokeWidth = t.s(4f)
    )

    // Cloaked body with a belt
    drawRoundRect(
        AvatarCloak,
        topLeft = Offset(t.x(x - 8f), t.y(y - 26f + bob)),
        size = Size(t.s(16f), t.s(18f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(t.s(6f))
    )
    drawRect(
        AvatarTrim,
        topLeft = Offset(t.x(x - 8f), t.y(y - 14f + bob)),
        size = Size(t.s(16f), t.s(2f))
    )

    // Head, hair, and a facing-direction eye
    drawCircle(AvatarSkin, t.s(6.5f), Offset(t.x(x), t.y(y - 30f + bob)))
    drawArc(
        AvatarHair,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(t.x(x - 6.8f), t.y(y - 37.2f + bob)),
        size = Size(t.s(13.6f), t.s(13.6f))
    )
    drawCircle(Color(0xFF25313D), t.s(1.2f), Offset(t.x(x + facing * 3f), t.y(y - 29.5f + bob)))
}

/** Draws an emoji centered at (cx, cy) using the native canvas. */
private fun DrawScope.drawEmoji(text: String, cx: Float, cy: Float, sizePx: Float) {
    if (text.isEmpty() || sizePx <= 1f) return
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val metrics = paint.fontMetrics
    drawContext.canvas.nativeCanvas.drawText(text, cx, cy - (metrics.ascent + metrics.descent) / 2f, paint)
}

/** Draws a small shadowed white label centered at (cx, top). */
private fun DrawScope.drawMapLabel(text: String, cx: Float, top: Float, sizePx: Float) {
    if (text.isEmpty()) return
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx.coerceAtLeast(9f)
        textAlign = android.graphics.Paint.Align.CENTER
        color = android.graphics.Color.WHITE
        setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(text, cx, top, paint)
}

/** Parses a #RRGGBB theme color, falling back on bad input so maps never crash the UI. */
private fun parseMapColor(hex: String, fallback: Color): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: IllegalArgumentException) {
    fallback
}

private fun darken(color: Color, factor: Float): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)
