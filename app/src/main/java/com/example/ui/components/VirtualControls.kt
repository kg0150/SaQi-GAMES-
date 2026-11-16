package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActionContextType
import com.example.data.HudSnapshot
import com.example.localization.LocalizedStrings
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.PatrolCobalt
import com.example.ui.theme.PoliceGold
import com.example.ui.theme.SirenBlue
import com.example.ui.theme.SirenRed
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import kotlin.math.hypot

@Composable
fun VirtualJoystick(
    onStickMoved: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val maxRadius = this.size.width * 0.36f
                detectDragGestures(
                    onDragStart = { offset ->
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = hypot(dx, dy).coerceAtLeast(0.001f)
                        val clamped = dist.coerceAtMost(maxRadius)
                        knobX = (dx / dist) * clamped
                        knobY = (dy / dist) * clamped
                        onStickMoved(knobX / maxRadius, knobY / maxRadius)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nx = knobX + dragAmount.x
                        val ny = knobY + dragAmount.y
                        val dist = hypot(nx, ny).coerceAtLeast(0.001f)
                        val clamped = dist.coerceAtMost(maxRadius)
                        knobX = (nx / dist) * clamped
                        knobY = (ny / dist) * clamped
                        onStickMoved(knobX / maxRadius, knobY / maxRadius)
                    },
                    onDragEnd = {
                        knobX = 0f
                        knobY = 0f
                        onStickMoved(0f, 0f)
                    },
                    onDragCancel = {
                        knobX = 0f
                        knobY = 0f
                        onStickMoved(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.width * 0.44f
            val innerR = this.size.width * 0.19f

            // Outer translucent tactical ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ObsidianNavy.copy(alpha = 0.25f),
                        TacticalSlate.copy(alpha = 0.62f)
                    ),
                    center = center,
                    radius = outerR
                ),
                radius = outerR,
                center = center
            )
            drawCircle(
                color = PatrolCobalt.copy(alpha = 0.65f),
                radius = outerR,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = outerR * 0.65f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Thumb puck
            val puckCenter = Offset(center.x + knobX, center.y + knobY)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SirenBlue, PatrolCobalt),
                    center = puckCenter,
                    radius = innerR
                ),
                radius = innerR,
                center = puckCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = innerR,
                center = puckCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun ActionControlsCluster(
    hud: HudSnapshot,
    strings: LocalizedStrings,
    isRunning: Boolean,
    onRunToggle: (Boolean) -> Unit,
    onJump: () -> Unit,
    onActionOrArrest: () -> Unit,
    onEnterExitVehicle: () -> Unit,
    onSirenToggle: () -> Unit,
    onGasChanged: (Boolean) -> Unit,
    onBrakeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hud.isInVehicle) {
            // Enter Vehicle prompt button when near a police cruiser
            AnimatedVisibility(visible = hud.canEnterVehicle) {
                TacticalActionPill(
                    label = strings.enterCarBtn,
                    icon = Icons.Rounded.DirectionsCar,
                    accent = SirenBlue,
                    onClick = onEnterExitVehicle,
                    testTag = "enter_vehicle_button"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Run Toggle Button
                TacticalRoundButton(
                    icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                    label = strings.runBtn,
                    accent = if (isRunning) PoliceGold else PatrolCobalt,
                    filled = isRunning,
                    size = 58.dp,
                    onClick = { onRunToggle(!isRunning) },
                    testTag = "run_button"
                )

                // Jump Button
                TacticalRoundButton(
                    icon = Icons.Rounded.ArrowUpward,
                    label = strings.jumpBtn,
                    accent = PatrolCobalt,
                    filled = false,
                    size = 58.dp,
                    onClick = onJump,
                    testTag = "jump_button"
                )

                // Context Primary Action / Arrest / Talk Button
                val (actIcon, actLabel, actColor) = when (hud.actionContext) {
                    ActionContextType.ARREST -> Triple(Icons.Rounded.Gavel, strings.arrestBtn, SirenRed)
                    ActionContextType.TALK -> Triple(Icons.Rounded.RecordVoiceOver, strings.talkBtn, EmeraldSignal)
                    ActionContextType.INTERACT -> Triple(Icons.Rounded.Gavel, strings.actionBtn, PoliceGold)
                    ActionContextType.NONE -> Triple(Icons.Rounded.Gavel, strings.actionBtn, PatrolCobalt)
                }
                TacticalRoundButton(
                    icon = actIcon,
                    label = actLabel,
                    accent = actColor,
                    filled = hud.actionContext != ActionContextType.NONE,
                    size = 70.dp,
                    onClick = onActionOrArrest,
                    testTag = "action_button"
                )
            }
        } else {
            // Driving Controls Cluster
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TacticalActionPill(
                    label = strings.sirenBtn,
                    icon = Icons.Rounded.Campaign,
                    accent = if (hud.sirenOn) SirenRed else PatrolCobalt,
                    onClick = onSirenToggle,
                    testTag = "siren_button"
                )
                TacticalActionPill(
                    label = strings.exitCarBtn,
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    accent = PoliceGold,
                    onClick = onEnterExitVehicle,
                    testTag = "exit_vehicle_button"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                HoldActionPad(
                    icon = Icons.Rounded.StopCircle,
                    label = strings.brakeBtn,
                    accent = SirenRed,
                    size = 64.dp,
                    onHoldChanged = onBrakeChanged,
                    testTag = "brake_button"
                )
                HoldActionPad(
                    icon = Icons.Rounded.Speed,
                    label = strings.gasBtn,
                    accent = EmeraldSignal,
                    size = 76.dp,
                    onHoldChanged = onGasChanged,
                    testTag = "gas_button"
                )
            }
        }
    }
}

@Composable
private fun TacticalRoundButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    filled: Boolean,
    size: Dp,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (filled) accent.copy(alpha = 0.88f) else TacticalSlate.copy(alpha = 0.78f),
        label = "btnBg"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, accent, CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(size * 0.46f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HoldActionPad(
    icon: ImageVector,
    label: String,
    accent: Color,
    size: Dp,
    onHoldChanged: (Boolean) -> Unit,
    testTag: String
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (pressed) accent.copy(alpha = 0.9f) else TacticalSlate.copy(alpha = 0.82f)
                )
                .border(2.dp, accent, RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onHoldChanged(true)
                            tryAwaitRelease()
                            onHoldChanged(false)
                        }
                    )
                }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(size * 0.45f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun TacticalActionPill(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TacticalSlate.copy(alpha = 0.88f))
            .border(1.5.dp, accent, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
    }
}
