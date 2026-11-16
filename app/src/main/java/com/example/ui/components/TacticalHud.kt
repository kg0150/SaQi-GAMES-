package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BlipType
import com.example.data.DistrictId
import com.example.data.HudSnapshot
import com.example.data.OfficerId
import com.example.localization.LocalizedStrings
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PatrolCobalt
import com.example.ui.theme.PoliceGold
import com.example.ui.theme.SirenBlue
import com.example.ui.theme.SirenRed
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TacticalHud(
    hud: HudSnapshot,
    totalScore: Int,
    strings: LocalizedStrings,
    isRunning: Boolean,
    onJoystickMoved: (Float, Float) -> Unit,
    onRunToggle: (Boolean) -> Unit,
    onJump: () -> Unit,
    onActionOrArrest: () -> Unit,
    onEnterExitVehicle: () -> Unit,
    onSirenToggle: () -> Unit,
    onGasChanged: (Boolean) -> Unit,
    onBrakeChanged: (Boolean) -> Unit,
    onSwitchOfficer: () -> Unit,
    onOpenMissions: () -> Unit,
    onPause: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle cinematic letterbox bars during mission intro sweep
        AnimatedVisibility(
            visible = hud.isCinematicIntro,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(Color.Black.copy(alpha = 0.85f)))
                Box(modifier = Modifier.fillMaxWidth().height(42.dp).background(Color.Black.copy(alpha = 0.85f)))
            }
        }

        // Main Safe-Area HUD Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Officer Status + Objective + Radar/Pause
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Top-Left: Officer Card + Health/Stamina + Switch
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.widthIn(max = 220.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(TacticalSlate.copy(alpha = 0.82f))
                            .border(1.dp, PanelBorder, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val portraitRes = if (hud.officer == OfficerId.MOTU) {
                            R.drawable.img_motu_portrait
                        } else {
                            R.drawable.img_patlu_portrait
                        }
                        Image(
                            painter = painterResource(id = portraitRes),
                            contentDescription = if (hud.officer == OfficerId.MOTU) strings.motuName else strings.patluName,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, PoliceGold, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = if (hud.officer == OfficerId.MOTU) strings.motuName else strings.patluName,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { (hud.health / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = EmeraldSignal,
                                trackColor = ObsidianNavy
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { (hud.stamina / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(3.dp)),
                                color = SirenBlue,
                                trackColor = ObsidianNavy
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Score Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TacticalSlate.copy(alpha = 0.80f))
                                .border(1.dp, PoliceGold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${strings.totalScore}: ${totalScore + hud.scoreInSession}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PoliceGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Switch Officer Quick Chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PatrolCobalt.copy(alpha = 0.82f))
                                .clickable(onClick = onSwitchOfficer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("switch_officer_hud"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = strings.switchOfficer,
                                tint = TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = strings.switchOfficer,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Top-Center: District + Mission Objective Banner
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalSlate.copy(alpha = 0.80f))
                            .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = SirenBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = districtLabel(hud.district, strings),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hud.activeMissionId > 0) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = if (hud.timeRemainingSec < 25f) SirenRed else PoliceGold,
                                modifier = Modifier.size(15.dp)
                            )
                            val mins = (hud.timeRemainingSec.toInt() / 60)
                            val secs = (hud.timeRemainingSec.toInt() % 60)
                            Text(
                                text = "%d:%02d".format(mins, secs),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (hud.timeRemainingSec < 25f) SirenRed else PoliceGold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.WbSunny,
                                contentDescription = null,
                                tint = PoliceGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Active Objective Strip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianNavy.copy(alpha = 0.78f))
                            .border(1.dp, PatrolCobalt.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = resolveObjectiveText(hud.objectiveTextKey, strings),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hud.isInVehicle) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianNavy.copy(alpha = 0.75f))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${hud.speedKmh} ${strings.speedUnit}",
                                style = MaterialTheme.typography.titleMedium,
                                color = SirenBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Top-Right: Mini-Radar + Missions + Pause
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    MiniRadar(hud = hud, modifier = Modifier.size(74.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TacticalSlate.copy(alpha = 0.85f))
                                .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                                .testTag("pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = strings.pauseTitle,
                                tint = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = onOpenMissions,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TacticalSlate.copy(alpha = 0.85f))
                                .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                                .testTag("hud_missions_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Assignment,
                                contentDescription = strings.missions,
                                tint = PoliceGold
                            )
                        }
                    }
                }
            }

            // MIDDLE DIALOGUE BANNER
            AnimatedVisibility(
                visible = hud.dialogueText != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                val msg = when (hud.dialogueText) {
                    "dialogue_motu" -> strings.dialogueMotuStart
                    "dialogue_patlu" -> strings.dialoguePatluStart
                    "dialogue_civ" -> strings.dialogueCivilianThanks
                    "dialogue_arrest" -> strings.dialogueSuspectCaught
                    else -> ""
                }
                if (msg.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianNavy.copy(alpha = 0.88f))
                            .border(1.5.dp, PoliceGold, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // BOTTOM CONTROLS: Virtual Joystick (Left) + Action Cluster (Right)
            if (!hud.isCinematicIntro) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    VirtualJoystick(onStickMoved = onJoystickMoved)
                    ActionControlsCluster(
                        hud = hud,
                        strings = strings,
                        isRunning = isRunning,
                        onRunToggle = onRunToggle,
                        onJump = onJump,
                        onActionOrArrest = onActionOrArrest,
                        onEnterExitVehicle = onEnterExitVehicle,
                        onSirenToggle = onSirenToggle,
                        onGasChanged = onGasChanged,
                        onBrakeChanged = onBrakeChanged
                    )
                }
            }
        }
    }
}

@Composable
fun MiniRadar(hud: HudSnapshot, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(ObsidianNavy.copy(alpha = 0.86f))
            .border(2.dp, PatrolCobalt, CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(r, r)
            val worldScale = r / 85f

            // District quadrants grid lines
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(0f, r),
                end = Offset(size.width, r),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(r, 0f),
                end = Offset(r, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = r * 0.55f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Blips relative to player position
            for (blip in hud.radarBlips) {
                val dx = (blip.x - hud.playerX) * worldScale
                val dz = (blip.z - hud.playerZ) * worldScale
                val dist = kotlin.math.hypot(dx, dz)
                val maxB = r * 0.86f
                val bx = if (dist > maxB) (dx / dist) * maxB else dx
                val bz = if (dist > maxB) (dz / dist) * maxB else dz
                val color = when (blip.type) {
                    BlipType.PRECINCT -> PatrolCobalt
                    BlipType.OBJECTIVE -> PoliceGold
                    BlipType.CRIMINAL -> SirenRed
                    BlipType.POLICE_CAR -> SirenBlue
                    BlipType.CIVILIAN -> EmeraldSignal
                }
                val blipR = if (blip.type == BlipType.OBJECTIVE || blip.type == BlipType.CRIMINAL) 4.dp.toPx() else 2.8.dp.toPx()
                drawCircle(
                    color = color,
                    radius = blipR,
                    center = Offset(center.x + bx, center.y + bz)
                )
            }

            // Player directional chevron at center
            val yaw = hud.playerYawRad
            val tip = Offset(center.x + sin(yaw) * 7.dp.toPx(), center.y + cos(yaw) * 7.dp.toPx())
            val left = Offset(center.x + sin(yaw + 2.4f) * 5.dp.toPx(), center.y + cos(yaw + 2.4f) * 5.dp.toPx())
            val right = Offset(center.x + sin(yaw - 2.4f) * 5.dp.toPx(), center.y + cos(yaw - 2.4f) * 5.dp.toPx())
            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            drawPath(path, color = Color.White)
        }
    }
}

fun districtLabel(district: DistrictId, strings: LocalizedStrings): String = when (district) {
    DistrictId.PRECINCT -> strings.districtPrecinct
    DistrictId.BAZAAR -> strings.districtBazaar
    DistrictId.PARK -> strings.districtPark
    DistrictId.DOWNTOWN -> strings.districtDowntown
}

private fun resolveObjectiveText(key: String, strings: LocalizedStrings): String = when (key) {
    "m1_1" -> strings.mission1Obj1
    "m1_2" -> strings.mission1Obj2
    "m1_3" -> strings.mission1Obj3
    "m2_1" -> strings.mission2Obj1
    "m2_2" -> strings.mission2Obj2
    "m2_3" -> strings.mission2Obj3
    "m3_1" -> strings.mission3Obj1
    "m3_2" -> strings.mission3Obj2
    "m3_3" -> strings.mission3Obj3
    "m4_1" -> strings.mission4Obj1
    "m4_2" -> strings.mission4Obj2
    "m4_3" -> strings.mission4Obj3
    "m5_1" -> strings.mission5Obj1
    "m5_2" -> strings.mission5Obj2
    "m5_3" -> strings.mission5Obj3
    else -> strings.freePatrolObjective
}
